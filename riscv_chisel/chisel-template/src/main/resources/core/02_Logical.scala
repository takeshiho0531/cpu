// package commonのInstructionsオブジェクトの全てのメンバ
import common.Instructions._

val io = IO(new Bundle {
    val imem = Flipped(new ImemPortIo())
    val dmem = Flipped(new DmemPortIo())
    val exit = Output(Bool())
})

val imm_i = inst(31, 20)  // offset[11:0]の抽出
val imm_i_sect = Cat(Fill(20, imm_i(11)), imm_i)  // offsetの符号拡張

val imm_s = Cat(inst(31, 25), inst(11, 7))
val imm_s_sext = Cat(Fill(20, imm_s(11)), imm_s)

val alu_out = MuxCase(0.U(WORD_LEN.W), Seq(
    (inst === LW || inst === ADDI) -> (rs1_data + imm_i_sect) // メモリアドレスの計算
    (inst === SW) -> (rs1_data + imm_s_sext),
    (inst === ADD) -> (rs1_data + rs2_data),
    (inst === SUB) -> (rs1_data - rs2_data),
    (inst === AND) -> (rs1_data & rs2_data), // 追加
    (inst === OR) -> (rs1_data | rs2_data), // 追加
    (inst === XOR) -> (rs1_data ^ rs2_data), // 追加
    (inst === ANDI) -> (rs1_data & imm_i_sext), // 追加
    (inst === ORI) -> (rs1_data | rs2_data), // 追加
    (inst === XORI) -> (rs1_data ^ imm_s_sext) // 追加
))

io.dmem.addr := alu_out

io.dmem.wen := (inst === SW)
io.dmem.wdata := rs2_data

val wb_data = MuxCase(alu_out, Seq(
    (inst === LW) -> io.dmem.rdata
))

when (inst === LW || inst === ADD || inst === ADDI || inst ===SUB ||
inst === AND || inst === OR || inst === XOR || inst === ADDI || inst === ORI || inst === XORI) {  // 追加
    regfile(wb_addr) := wb_data
}