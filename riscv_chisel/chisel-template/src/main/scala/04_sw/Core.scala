// package commonのInstructionsオブジェクトの全てのメンバ
import common.Instructions._

val io = IO(new Bundle {
    val imem = Flipped(new ImemPortIo())
    val dmem = Flipped(new DmemPortIo())  // 追加
    val exit = Output(Bool())
})

val imm_i = inst(31, 20)  // offset[11:0]の抽出
val imm_i_sect = Cat(Fill(20, imm_i(11)), imm_i)  // offsetの符号拡張

val imm_s = Cat(inst(31, 25), inst(11, 7)) //　追加
val imm_s_sext = Cat(Fill(20, imm_s(11)), imm_s) // 追加

val alu_out = MuxCase(0.U(WORD_LEN.W), Seq(
    (inst === LW) -> (rs1_data + imm_i_sect) // メモリアドレスの計算
    (inst === SW) -> (rs1_data + imm_s_sext) // 追加
))

io.dmem.addr := alu_out

io.dmem.wen := (inst === SW) // 追加
io.dmem.wdata := rs2_data  // 追加

val wb_data = io.dmem.rdata

when (inst === LW) {
    regfile(wb_addr) := wb_data
}