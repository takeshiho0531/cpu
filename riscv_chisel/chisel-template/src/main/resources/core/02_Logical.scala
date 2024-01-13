val alu_out = MuxCase(0.U(WORD_LEN.W), Seq(
    (inst === LW || inst === ADDI) -> (rs1_data + imm_i_sext), // ADDI追加
    (inst === SW) -> (rs1_data + imm_s_sext),
    (inst === ADD) -> (rs1_data + rs2_data),
    (inst === SUB) -> (rs1_data - rs2_data),
    (inst === AND) -> (rs1_data & rs2_data), // 追加
    (inst === OR) -> (rs1_data | rs2_data), // 追加
    (inst === XOR) -> (rs1_data ^ rs2_data), // 追加
    (inst === ANDI) -> (rs1_data & imm_i_sext), // 追加
    (inst === ORI) -> (rs1_data | imm_i_sext), // 追加
    (inst === XORI) -> (rs1_data ^ imm_i_sext) // 追加
))

when (inst === LW || inst === ADD || inst === ADDI || inst === SUB || 
inst === AND || inst === OR || inst === XOR || inst === ADDI || inst === ORI || inst === XORI) {
    regfile(wb_addr) := wb_data
}