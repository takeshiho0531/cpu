val csignals = ListLookup(inst, 
        List(ALU_X, OP1_RS1, OP2_RS2, MEN_X, REN_X, WB_X),

    Array(
        LW -> List(ALU_ADD, OP1_RS1, OP2_IMI, MEN_X, REN_S, WB_MEM),
        SW -> List(ALU_ADD, OP1_RS1, OP2_IMS, MEN_S, REN_X, WB_X),
        ADD -> List(ALU_ADD, OP1_RS1, OP2_RS2, MEN_X, REN_S, WB_ALU),
        ADDI -> List(ALU_ADD, OP1_RS1, OP2_IMI, MEN_X, REN_S, WB_ALU),
        SUB -> List(ALU_SUB, OP1_RS1, OP2_RS2, MEN_X, REN_S, WB_ALU),
        AND -> List(ALU_AND, OP1_RS1, OP2_RS2, MEN_X, REN_S, WB_ALU),
        OR -> List(ALU_OR, OP1_RS1, OP2_RS2, MEN_X, REN_S, WB_ALU),
        XOR -> List(ALU_XOR, OP1_RS1, OP2_RS2, MEN_X, REN_S, WB_ALU),
        ANDI -> List(ALU_AND, OP1_RS1, OP2_IMI, MEN_X, REN_S, WB_ALU),
        ORI -> List(ALU_OR, OP1_RS1, OP2_IMI, MEN_X, REN_S, WB_ALU),
        XORI -> List(XOR, OP1_RS1, OP2_IMI, MEN_X, REN_S, WB_ALU),
        SLL -> List(ALU_SLL, OP1_RS1, OP2_RS2, MEN_X, REN_S, WB_ALU),
        SRL -> List(ALU_SRL, OP1_RS1, OP2_RS2, MEN_X, REN_S, WB_ALU),
        SRA -> List(ALU_SRA, OP1_RS1, OP2_RS2, MEN_X, REN_S, WB_ALU),
        SLLI -> List(ALU_SLL, OP1_RS1, OP2_IMI, MEN_X, REN_S, WB_ALU),
        SRLI -> List(ALU_SRL, OP1_RS1, OP2_IMI, MEN_X, REN_S, WB_ALU),
        SRAI -> List(ALU_SRA, OP1_RS1, OP2_IMI, MEN_X, REN_S, WB_ALU)
    )
)

val exe_fun :: op1_sel :: op2_sel :: mem_wen :: rf_wen :: wb_sel :: Nil = csignals

val op1_data = MuxCase(0.U(WORD_LEN.W), Seq(
    (op1_sel === OP1_RS1) -> rs1_data
))

val op2_data = MuxCase(0.U(WORD_LEN.W), Seq(
    (op2_sel === OP2_RS2) -> rs2_data,
    (op2_sel === OP2_IMI) -> imm_i_sext,
    (op2_sel === OP2_IMS) -> imm_s_sext
))

val alu_out = MuxCase(0.U(WORD_LEN.W), Seq(
    (exe_fun === ALU_ADD) -> (op1_data + op2_data),
    (exe_fun === ALU_SUB) -> (op1_data - op2_data),
    (exe_fun === ALU_AND) -> (op1_data & op2_data),
    (exe_fun === ALU_OR) -> (op1_data | op2_data),
    (exe_fun === ALU_XOR) -> (op1_data ^ op2_data),
    (exe_fun === ALU_SLL) -> (op1_data << op2_data(4,0))(31,0),
    (exe_fun === ALU_SRL) -> (op1_data >> op2_data(4,0))(31,0),
    (exe_fun === ALU_SRA) -> (op1_data.asSInt() >> op2_data(4,0)).asUInt()
))

io.dmem.wen := mem_wen

val wb_data = MuxCase(alu_out, Seq(
    (wb_sel === WB_MEM) -> io.dmem.rdata
))
when(rf_wen === REN_S) {
    regfile(wb_addr) := wb_data
}