val alu_out = MuxCase(0.U(WORD_LEN.W), Seq(
    (inst === LW || inst === ADDI) -> (rs1_data + imm_i_sext), // ADDI追加
    (inst === SW) -> (rs1_data + imm_s_sext),
    (inst === ADD) -> (rs1_data + rs2_data), // 追加
    (inst === SUB) -> (rs1_data - rs2_data) // 追加
))

val wb_data = MuxCase(alu_out, Seq(
    (inst === LW) -> io.dmem.rdata
))

when (inst === LW || inst === ADD || inst === ADDI || inst === SUB) {
    regfile(wb_addr) := wb_data
}