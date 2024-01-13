class DmemPortIo extends Bundle {
    val addr = Input(UInt(WORD_LEN.W))
    val rdata = Output(UInt(WORD_LEN.W))

    val wen = Input(Bool()) // 追加
    val wdata = Input(UInt(WORD_LEN.W)) // 追加
}

when (io.mem.wen) {
    mem(io.dmem.addr) := io.dmem.wdata(7,0)
    mem(io.dmem.addr + 1.U) := io.dmem.wdata(15,8)
    mem(io.dmem.addr + 2.U) := io.dmem.wdata(23,16)
    mem(io.dmem.addr + 3.U) := io.dmem.wdata(31,24)
}