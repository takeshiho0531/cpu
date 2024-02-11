import common.Instructions._


val pc_plus4 = pc_reg + 4.U(WORD_LEN.W)
val br_flg = Wire(Bool())
val br_target = Wire(UInt(WORD_LEN.W))

val jmp_flg = (inst === JAL || inst === JALR)
val alu_out = Wire(UInt(WORD_LEN.W))

val pc_next = MuxCase(pc_plus4, Seq(
    br_flg -> br_target,
    jmp_flg -> alu_out
))

pc_reg := pc_next

val imm_b = Cat(inst(31), inst(7), inst(30, 25), inst(11, 8))
val imm_b_sext = Cat(Fill(19, imm_b(11)), imm_b, 0.U(1.U))

val io = IO(new Bundle {
    val imem = Flipped(new ImemPortIo())
    val dmem = Flipped(new DmemPortIo())
    val exit = Output(Bool())
})

val imm_i = inst(31, 20)
val imm_i_sext = Cat(Fill(20, imm_i(11)), imm_i)
val imm_s = Cat(inst(31, 25), inst(11, 7))
val imm_s_sext = Cat(Fill(20, imm_s(11)), imm_s)
val imm_j = Cat(inst(31), inst(19, 12), inst(20), inst(30, 21))
val imm_j_sext = cat(Fill(11, imm_j(19)), imm_j, 0.U(1.U)) // 最下位bitを0に

val imm_u = inst(31, 12)
val imm_u_shifted = Cat(imm_u, Fill(12, 0.U))

val imm_z = inst(19, 15)  // 追加
val imm_z_uext = Cat(Fill(27, 0.U), imm_z) // 追加

val csignals = ListLookup(inst,
        List(ALU_X, OP1_RS1, OP2_RS2, MEN_X, REN_X, WB_X, CSR_X),

    Array(
        LW -> List(ALU_ADD, OP1_RS1, OP2_IMI, MEN_X, REN_S, WB_MEM, CSR_X),
        SW -> List(ALU_ADD, OP1_RS1, OP2_IMS, MEN_S, REN_X, WB_X, CSR_X),
        ADD -> List(ALU_ADD, OP1_RS1, OP2_RS2, MEN_X, REN_S, WB_ALU, CSR_X),
        ADDI -> List(ALU_ADD, OP1_RS1, OP2_IMI, MEN_X, REN_S, WB_ALU, CSR_X),
        SUB -> List(ALU_SUB, OP1_RS1, OP2_RS2, MEN_X, REN_S, WB_ALU, CSR_X),
        AND -> List(ALU_AND, OP1_RS1, OP2_RS2, MEN_X, REN_S, WB_ALU, CSR_X),
        OR -> List(ALU_OR, OP1_RS1, OP2_RS2, MEN_X, REN_S, WB_ALU, CSR_X),
        XOR -> List(ALU_XOR, OP1_RS1, OP2_RS2, MEN_X, REN_S, WB_ALU, CSR_X),
        ANDI -> List(ALU_AND, OP1_RS1, OP2_IMI, MEN_X, REN_S, WB_ALU, CSR_X),
        ORI -> List(ALU_OR, OP1_RS1, OP2_IMI, MEN_X, REN_S, WB_ALU, CSR_X),
        XORI -> List(XOR, OP1_RS1, OP2_IMI, MEN_X, REN_S, WB_ALU, CSR_X),
        SLL -> List(ALU_SLL, OP1_RS1, OP2_RS2, MEN_X, REN_S, WB_ALU, CSR_X),
        SRL -> List(ALU_SRL, OP1_RS1, OP2_RS2, MEN_X, REN_S, WB_ALU, CSR_X),
        SRA -> List(ALU_SRA, OP1_RS1, OP2_RS2, MEN_X, REN_S, WB_ALU, CSR_X),
        SLLI -> List(ALU_SLL, OP1_RS1, OP2_IMI, MEN_X, REN_S, WB_ALU, CSR_X),
        SRLI -> List(ALU_SRL, OP1_RS1, OP2_IMI, MEN_X, REN_S, WB_ALU, CSR_X),
        SRAI -> List(ALU_SRA, OP1_RS1, OP2_IMI, MEN_X, REN_S, WB_ALU, CSR_X),
        SLT -> List(ALU_SLT, OP1_RS1, OP2_RS2, MEN_X, REN_S, WB_ALU, CSR_X),
        SLTU -> List(ALU_SLTU, OP1_RS1, OP2_RS2, MEN_X, REN_S, WB_ALU, CSR_X),
        SLTI -> List(ALU_SLT, OP1_RS1, OP2_IMI, MEN_X, REN_S, WB_ALU, CSR_X),
        SLTIU -> List(ALU_SLTU, OP1_RS1, OP2_IMI, MEN_X, REN_S, WB_ALU, CSR_X),
        BEQ -> List(BR_BEQ, OP1_RS1, OP2_RS2, MEN_X, REN_X, WB_X, CSR_X),
        BNE -> List(BR_BNE, OP1_RS1, OP2_RS2, MEN_X, REN_X, WB_X, CSR_X),
        BGE -> List(BR_BGE, OP1_RS1, OP2_RS2, MEN_X, REN_X, WB_X, CSR_X),
        BGEU -> List(BR_BGEU, OP1_RS1, OP2_RS2, MEN_X, REN_X, WB_X, CSR_X),
        BLT -> List(BR_BLT, OP1_RS1, OP2_RS2, MEN_X, REN_X, WB_X, CSR_X),
        BLTU -> List(BR_BLTU, OP1_RS1, OP2_RS2, MEN_X, REN_X, WB_X, CSR_X),
        JAL -> List(ALU_ADD, OP1_PC, OP2_IMJ, MEN_X, REN_S, WB_PC, CSR_X),
        JALR -> List(ALU_JALR, OP1_RS1, OP2_IMI, MEN_X, REN_S, WB_PC, CSR_X),
        LUI -> List(ALU_ADD, OP1_X, OP2_IMU, MEN_X, REN_S, WB_ALU, CSR_X),
        AUIPC -> List(ALU_ADD, OP1_PC, OP2_IMU, MEN_X, REN_S, WB_ALU, CSR_X),
        CSRRW -> List(ALU_COPY1, OP1_RS1, OP2_X, MEN_X, REN_S, WB_CSR, CSR_W),  // 追加
        CSRRWI -> List(ALU_COPY1, OP1_IMZ, OP2_X, MEN_X, REN_S, WB_CSR, CSR_W), // 追加
        CSRRS -> List(ALU_COPY1, OP1_RS1, OP2_X, MEN_X, REN_S, WB_CSR, CSR_S), // 追加
        CSRRSI -> List(ALU_COPY1, OP1_IMZ, OP2_X, MEN_X, REN_S, WB_CSR, CSR_S), // 追加
        CSSRC -> List(ALU_COPY1, OP1_RS1, OP2_X, MEN_X, REN_S, WB_CSR, CSR_C), // 追加
        CSSRCI -> List(ALU_COPY1, OP1_IMZ, OP2_X, MEN_X, REN_S, WB_CSR, CSR_C) // 追加
    )
)

val exe_fun :: op1_sel :: op2_sel :: mem_wen :: rf_wen :: wb_sel :: csr_cmd :: Nil = csignals

val op1_data = MuxCase(0.U(WORD_LEN.W), Seq(
    (op1_sel === OP1_RS1) -> rs1_data,
    (op1_sel === OP1_PC) -> pc_reg //　追加
))

val op2_data = MuxCase(0.U(WORD_LEN.W), Seq(
    (op2_sel === OP2_RS2) -> rs2_data,
    (op2_sel === OP2_IMI) -> imm_i_sext,
    (op2_sel === OP2_IMS) -> imm_s_sext,
    (op2_sel === OP2_IMJ) -> imm_j_sext,
    (op1_sel === OP2_IMU) -> imm_u_shifted // 追加
))

alu_out := MuxCase(0.U(WORD_LEN.W), Seq(
    (exe_fun === ALU_ADD) -> (op1_data + op2_data),
    (exe_fun === ALU_SUB) -> (op1_data - op2_data),
    (exe_fun === ALU_AND) -> (op1_data & op2_data),
    (exe_fun === ALU_OR) -> (op1_data | op2_data),
    (exe_fun === ALU_XOR) -> (op1_data ^ op2_data),
    (exe_fun === ALU_SLL) -> (op1_data << op2_data(4,0))(31,0),
    (exe_fun === ALU_SRL) -> (op1_data >> op2_data(4,0))(31,0),
    (exe_fun === ALU_SRA) -> (op1_data.asSInt() >> op2_data(4,0)).asUInt(),
    (exe_fun === ALU_SLT) -> (op1_data.asSInt() < op2_data.asSInt()).asUInt(),
    (exe_fun === ALU_SLTU) -> (op1_data < op2_data).asUInt(),
    (exe_fun === ALU_JALR) -> (op1_data + op2_data) & ~1.U(WORD_LEN.W),
    (exe_fun === ALU_COPY1) -> op1_data // 追加
))

br_flg := MuxCase(false.B, Seq(
    (exe_fun === BR_BEQ) -> (op1_data === op2_data),
    (exe_fun === BR_BNE) -> !(op1_data === op2_data),
    (exe_fun === BR_BLT) -> (op1_data.asSInt() < op2_data.asSInt()),
    (exe_fun === BR_BGE) -> !(op1_data.asSInt() < op2_data.asSInt()),
    (exe_fun === BR_BLTU) -> (op1_data < op2_data),
    (exe_fun === BR_BGEU) -> !(op1_data < op2_data)
))

br_target := pc_reg + imm_b_sext

io.dmem.wen := mem_wen

val wb_data = MuxCase(alu_out, Seq(
    (wb_sel === WB_MEM) -> io.dmem.rdata,
    (wb_sel === WB_PC) -> pc_plus4,
    (wb_sel === WB_CSR) -> csr_rdata // 追加
))
when(rf_wen === REN_S) {
    regfile(wb_addr) := wb_data
}


io.dmem.addr := alu_out

io.dmem.wdata := rs2_data

val csr_regfile = Mem(4096, UInt(WORD_LEN.W)) // 追加
val csr_addr = inst(31,20) // 追加

// CSRの読み出し // 追加
csr_rdata = csr_regfile(csr_addr) // 追加

// CSRの書き込み // 追加
val csr_wdata = MuxCase(0.U(WORD_LEN.W), Seq(
    (csr_cmd === CSR_W) -> op1_data,
    (csr_cmd === CSR_S) -> (csr_rdata | op1_data),
    (csr_cmd === CSR_C) -> (csr_rdata & ~op1_data)
))

when (csr_cmd > 0.U) {  // CSR命令の時　 // 追加
    csr_regfile(csr_addr) := csr_wdata
}