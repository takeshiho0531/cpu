class DmemPortIo extends Bundle {
    val addr = Input(UInt(WORD_LEN.W))
    val rdata = Output(UInt(WORD_LEN.W))
}

val io = IO(new Bundle {
    val imem = new ImemPortIo()
    val dmem = new DmemPortIo() // 追加
})

io.dmem.rdata := Cat(
    mem(io.data.addr + 3.U(WORD_LEN.W)),
    mem(io.data.addr + 2.U(WORD_LEN.W)),
    mem(io.data.addr + 1.U(WORD_LEN.W)),
    mem(io.data.addr)
)