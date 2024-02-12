// **********
// Pipeline State Registers

// IF/ID State
val id_reg_pc = RegInit(0.U(WORD_LEN.W))
val id_reg_inst = RegInit(0.U(WORD_LEN.W))

// ID/EX State
val exe_reg_pc = RegInit(0.U(WORD_LEN.W))
val exe_reg_wb_addr = RegInit(0.U(ADDR_LEN.W))
val exe_reg_op1_data = RegInit(0.U(WORD_LEN.W))
val exe_reg_op2_data = RegInit(0.U(WORD_LEN.W))
val exe_reg_rs2_data = RegInit(0.U(WORD_LEN.W))
val exe_reg_exe_fun = RegInit(0.U(EXE_FUN_LEN.W))
val exe_reg_mem_wen = RegInit(0.U(MEM_LEN.W))
val exe_reg_rf_wen = RegInit(0.U(REN_LEN.W))
val exe_reg_wb_sel = RegInit(0.U(WB_SEL_LEN.W))
val exe_reg_csr_addr = RegInit(0.U(CSR_ADDR_LEN.W))
val exe_reg_csr_cmd = RegInit(0.U(CSR_LEN.W))
val exe_reg_imm_i_sext = RegInit(0.U(WORD_LEN.W))
val exe_reg_imm_s_sext = RegInit(0.U(WORD_LEN.W))
val exe_reg_imm_b_sext = RegInit(0.U(WORD_LEN.W))
val exe_reg_imm_u_shifted = RegInit(0.U(WORD_LEN.W))
val exe_reg_imm_z_uext = RegInit(0.U(WORD_LEN.W))

// EX/MEM State
val mem_reg_pc = RegInit(0.U(WORD_LEN.W))
val mem_reg_wb_addr = RegInit(0.U(ADDR_LEN.W))
val mem_reg_op1_data = RegInit(0.U(WORD_LEN.W))
val mem_reg_rs2_data = RegInit(0.U(WORD_LEN.W))
val mem_reg_mem_wen = RegInit(0.U(MEN_LEN.W))
val mem_reg_rf_wen = RegInit(0.U(REN_LEN.W))
val mem_reg_wb_sel = RegInit(0.U(WB_SEL_LEN.W))
val mem_reg_csr_addr = RegInit(0.U(CSR_ADDR_LEN.W))
val mem_reg_csr_cmd = RegInit(0.U(CSR_LEN.W))
val mem_reg_imm_z_uext = RegInit(0.U(WORD_LEN.W))
val mem_reg_alu_out = RegInit(0.U(WORD_LEN.W))

// MEM/WB State
val wb_reg_wb_addr = RegInit(0.U(ADDR_LEN.W))
val wb_reg_rf_wen = RegInit(0.U(REN_LEN.W))
val wb_reg_wb_data = RegInit(0.U(WORD_LEN.W))

// **********
// IF Stage
// 命令フェッチ・PC制御
val if_reg_pc = RegInit(START_ADDR)
io.imem.addr := if_reg_pc
val if_inst = io.imem.inst

val exe_br_flg = Wire(Bool())
val exw_br_target = Wire(UInt(WORD_LEN.W))
val exe_jmp_flg = Wire(Bool())
val exe_alu_out = Wire(UInt(WORD_LEN.W))
val mem_wb_data = Wire(UInt(WORD_LEN.W)) // 追加
val stall_flg = Wire(Bool()) // 追加

val if_pc_plus4 = if_reg_pc + 4.U(WORD_LEN.W)
val if_pc_next = MuxCase(if_pc_plus4, Seq(
    // 優先順位重要！ジャンプ成立とストールが同時発生した場合、ジャンプ処理を優先
    exe_br_flg          -> exe_br_target,
    exe_jmp_flg         -> exe_alu_out,
    (if_inst === ECALL) -> csr_regfile(0x305),
    stall_flg           -> if_reg_pc // 追加
))
if_reg_pc := if_pc_next

// IF/IDレジスタへの書き込み
id_reg_pc   := Mux(stall_flg, id_reg_pc, id_reg_pc) // 変更
id_reg_inst := MuxCase(if_inst, Seq(  // 変更
    // 優先順位重要！ジャンプ成立よストールが同時発生した場合、ジャンプ処理を優先
    (exe_br_flg || exe_jmp_flg) -> BUBBLE,
    stall_flg -> id_reg_inst
))

// **********
// ID Stage
// レジスタ番号のデコード・レジスタデータの読み出し
val id_inst = Mux((exe_br_flg || exe_jmp_flg || stall_flg), BUBBLE, id_reg_inst) // 変更
val id_rs1_addr = id_inst(19, 15)
val id_rs2_addr = id_inst(24, 20)
val id_wb_addr  = id_inst(11, 7)

val id_rs1_data_hazard = (exe_reg_rf_wen === REN_S) && (id_rs1_addr_b =/= 0.U) && (id_rs1_addr_b === exe_reg_wb_addr)
val id_rs2_data_hazard = (exe_reg_rf_wen === REN_S) && (id_rs2_addr_b =/= 0.U) && (id_rs2_addr_b === exe_reg_wb_addr)

stall_flg := (id_rs1_data_hazard || id_rs2_data_hazard)

val id_rs1_data = MuxCase(regfile(id_rs1_addr), Seq(
    (id_rs1_addr === 0.U) -> 0.U(WORD_LEN.W),

    // MEMからフォワーディング
    ((id_rs1_addr === mem_reg_wb_addr) && (mem_reg_rf_wen === REN_S)) -> mem_wb_data,

    // WBからフォワーディング
    ((id_rs1_addr === wb_reg_wb_addr) && (wb_reg_rf_wen === REN_S))   -> wb_reg_wb_data
))

val id_rs2_data = MuxCase(regfile(id_rs2_addr), Seq(
    (id_rs2_addr === 0.U) -> 0.U(WORD_LEN.W),
    ((id_rs2_addr === mem_reg_wb_addr) && (mem_reg_rf_wen === REN_S)) -> mem_wb_data,
    ((id_rs2_addr === wb_reg_wb_addr)  && (wb_reg_rf_wen  === REN_S)) -> wb_reg_wb_data
))

// 即値のデコード
val id_imm_i = id_inst(31, 20)
val id_imm_i_sext = Cat(Fill(20, id_imm_i(11)), id_imm_i)

val id_imm_s = Cat(id_inst(31, 25), id_inst(11, 7))
val id_imm_s_sext = Cat(Fill(20, id_imm_s(11)), id_imm_s)

val id_imm_b = Cat(id_inst(31), id_inst(7), id_inst(30, 25), id_inst(11, 8))
val id_imm_b_sext = Cat(Fill(19, id_imm_b(11)), id_imm_b, 0.U(1.U))

val id_imm_j = Cat(id_inst(31), id_inst(19, 12), id_inst(20), id_inst(30, 21))
val id_imm_j_sext = Cat(Fill(11, id_imm_j(19)), id_imm_j, 0.U(1.U))

val id_imm_u = id_inst(31, 12)
val id_imm_u_shifted = Cat(id_imm_u, Fill(12, 0.U))

val id_imm_z = id_inst(19, 15)
val id_imm_z_uext = Cat(Fill(27, 0.U), id_imm_z)

// csignalsのデコード
val csignals = ListLookup(id_reg_inst, ...)
val id_exe_fun :: id_op1_sel :: id_op2_sel :: id_mem_wen :: id_rf_wen :: id_wb_sel :: id_csr_cmd :: Nil = csignals

// オペランドデータの選択
val id_op1_data = MuxCase(0.U(WORD_LEN.W), Seq(
    (id_op1_sel === OP1_RS1) -> id_rs1_data,
    (id_op1_sel === OP1_PC)  -> id_reg_pc,
    (id_op1_sel === OP1_IMZ) -> id_imm_z_uext
))

val id_op2_data = MuxCase(0.U(WORD_LEN.W), Seq(
    (id_op2_sel === OP2_RS2) -> id_rs2_data,
    (id_op2_sel === OP2_IMI) -> id_imm_i_sext,
    (id_op2_sel === OP2_IMS) -> id_imm_s_sext,
    (id_op2_sel === OP2_IMJ) -> id_imm_j_sext,
    (id_op2_sel === OP2_IMU) -> id_imm_u_shifted
))

// csr_addrの生成
val id_csr_addr = Mux(id_csr_cmd === CSR_E, 0x342.U(CSR_ADD_LEN.W), id_inst(31,20)) // CSRのアドレス0x342はmcauseのレジスタ

// ID/EXレジスタへの書き込み
exe_reg_pc            := id_reg_pc
exe_reg_op1_data      := id_op1_data
exe_reg_op2_data      := id_op2_data
exe_reg_rs2_data      := id_rs2_data
exe_reg_wb_addr       := id_wb_addr
exe_reg_rf_wen        := id_rf_wen
exe_reg_exe_fun       := id_exe_fun
exe_reg_wb_sel        := id_wb_sel
exe_reg_imm_i_sext    := id_imm_i_sext
exe_reg_imm_s_sext    := id_imm_s_sext
exe_reg_imm_b_sext    := id_imm_b_sext
exe_reg_imm_u_shifted := id_imm_u_shifted
exe_reg_imm_z_uext    := id_imm_z_uext
exe_reg_csr_addr      := id_csr_addr
exe_reg_csr_cmd       := id_csr_cmd
exe_reg_mem_wen       := id_mem_wen

// **********
// EXステージ
// alu_outへの信号接続
exe_alu_out := MuxCase(0.U(WORD_LEN.W), Seq(
    (exe_reg_exe_fun === ALU_ADD)   -> (exe_reg_op1_data + exe_reg_op2_data),
    (exe_reg_exe_fun === ALU_SUB)   -> (exe_reg_op1_data - exe_reg_op2_data),
    (exe_reg_exe_fun === ALU_AND)   -> (exe_reg_op1_data & exe_reg_op2_data),
    (exe_reg_exe_fun === ALU_OR)    -> (exe_reg_op1_data | exe_reg_op2_data),
    (exe_reg_exe_fun === ALU_XOR)   -> (exe_reg_op1_data ^ exe_reg_op2_data),
    (exe_reg_exe_fun === ALU_SLL)   -> (exe_reg_op1_data << exe_reg_op2_data(4, 0)),
    (exe_reg_exe_fun === ALU_SRL)   -> (exe_reg_op1_data >> exe_reg_op2_data(4, 0)),
    (exe_reg_exe_fun === ALU_SRA)   -> (exe_reg_op1_data.asSInt() >> exe_reg_op2_data(4, 0).asSInt()),
    (exe_reg_exe_fun === ALU_SLT)   -> (exe_reg_op1_data.asSInt() < exe_reg_op2_data.asSInt()),
    (exe_reg_exe_fun === ALU_SLTU)  -> (exe_reg_op1_data.asSInt() < exe_reg_op2_data.asSInt()).asSInt(),
    (exe_reg_exe_fun === ALU_JALR)  -> ((exe_reg_op1_data + exe_reg_op2_data) & ~1.U(WORD_LEN.W)),
    (exe_reg_exe_fun === ALU_COPY1) -> exe_reg_op1_data
))

// 分岐命令の処理
exe_br_flg := MuxCase(false.B, Seq(
    (exe_reg_exe_fun === BR_BEQ) -> (exe_reg_op1_data === exe_reg_op2_data),
    (exe_reg_exe_fun === BR_BNE) -> !(exe_reg_op1_data === exe_reg_op2_data),
    (exe_reg_exe_fun === BR_BLT) -> (exe_reg_op1_data.asSInt() < exe_reg_op2_data.asSInt()),
    (exe_reg_exe_fun === BR_BGE) -> !(exe_reg_op1_data.asSInt() < exe_reg_op2_data.asSInt()),
    (exe_reg_exe_fun === BR_BLTU) -> (exe_reg_op1_data < exe_reg_op2_data),
    (exe_reg_exe_fun === BR_BGEU) -> !(exe_reg_op1_data < exe_reg_op2_data)
))
exe_br_target := exe_reg_pc + exe_reg_imm_b_sext

exe_jmp_flg := (exe_reg_wb_sel === WB_PC)

// EX/MEMレジスタへの書き込み
mem_reg_pc         := exe_reg_pc
mem_reg_op1_data   := exe_reg_op1_data
mem_reg_rs2_data   := exe_reg_rs2_data
mem_reg_wb_addr    := exe_reg_wb_addr
mem_reg_alu_out    := exe_alu_out
mem_reg_rf_wen     := exe_reg_rf_wen
mem_reg_wb_sel     := exe_reg_wb_sel
mem_reg_csr_addr   := exe_reg_csr_addr
mem_reg_csr_cmd    := exe_reg_csr_cmd
mem_reg_imm_z_uext := exe_reg_imm_z_uext
mem_reg_mem_wen    := exe_reg_mem_wen

// **********
// MEMステージ
// メモリアクセス
io.dmem.addr := mem_reg_alu_out
io.dmem.wen := mem_reg_mem_wen
io.dmem.wdata := mem_reg_rs2_data

// CSR
val csr_rdata = csr_regfile(mem_reg_csr_addr)

val csr_wdata = MuxCase(0.U(WORD_LEN.W), Seq(
    (mem_reg_csr_cmd === CSR_W) -> mem_reg_op1_data,
    (mem_reg_csr_cmd === CSR_S) -> (csr_rdata | mem_reg_op1_data),
    (mem_reg_csr_cmd === CSR_C) -> (csr_rdata & mem_reg_op1_data),
    (mem_reg_csr_cmd === CSR_E) -> 11.U(WORD_LEN.W)
))

when (mem_reg_csr_cmd > 0.U){
    csr_regfile(mem_reg_csr_addr) := csr_wdata
}

// wb_data
val mem_wb_data = MuxCase(mem_reg_alu_out, Seq(
    (mem_reg_wb_sel === WB_MEM) -> io.dmem.rdata,
    (mem_reg_wb_sel === WB_PC)  -> (mem_reg_pc + 4.U(WORD_LEN.W)),
    (mem_reg_wb_sel === WB_CSR) -> csr_rdata
))

// MEM/WBレジスタへの書き込み
wb_reg_wb_addr := mem_reg_wb_addr
wb_reg_rf_wen  := mem_reg_rf_wen
wb_reg_wb_data := mem_wb_data

// **********
// WBステージ
when (wb_reg_rf_wen === REN_S) {
    regfile(wb_reg_wb_addr) := wb_reg_wb_data
}

// **********
// debug code
printf(p"if_reg_pc       : 0x${Hexadecimal(if_reg_pc)}\n")
printf(p"id_reg_pc       : 0x${Hexadecimal(id_reg_pc)}\n")
printf(p"id_reg_inst     : 0x${Hexadecimal(id_reg_inst)}\n")
printf(p"id_inst         : 0x${Hexadecimal(id_inst)}\n")
printf(p"id_rs1_data     : 0x${Hexadecimal(id_rs1_data)}\n")
printf(p"id_rs2_data     : 0x${Hexadecimal(id_rs2_data)}\n")
printf(p"exe_reg_pc      : 0x${Hexadecimal(exe_reg_pc)}\n")
printf(p"exe_reg_op1_data: 0x${Hexadecimal(exe_reg_op1_data)}\n")
printf(p"exe_reg_op2_data: 0x${Hexadecimal(exe_reg_op2_data)}\n")
printf(p"exe_alu_out     : 0x${Hexadecimal(exe_alu_out)}\n")
printf(p"mem_reg_pc      : 0x${Hexadecimal(mem_reg_pc)}\n")
printf(p"mem_wb_data     : 0x${Hexadecimal(mem_wb_data)}\n")
printf(p"wb_reg_wb_data  : 0x${Hexadecimal(wb_reg_wb_data)}\n")
printf(p"stall_flg       : $stall_flg\n") // 追加