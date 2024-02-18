package vle

import chisel3._
import chisel3.util._
import common.Consts._
import chisel3.util.experimental.loadMemoryFromFile

class ImemPortIo extends Bundle {
    val addr = Input(UInt(WORD_LEN.W))
    val inst = Output(UInt(WORD_LEN.W))
}

class DmemPortIo extends Bundle {
    val addr   = Input(UInt(WORD_LEN.W))
    val rdata  = Output(UInt(WORD_LEN.W))
    val wen    = Input(UInt(WORD_LEN.W))
    val wdata  = Input(UInt(WORD_LEN.W))
    val vedata = Output(UInt(VLEN*8).W) // 追加
}

class Memory extends Module {
    val io = IO(new Bundle {
        val imem = new ImemPortIo()
        val dmem = new DmemPortIo()
    })

    val mem = Mem(16384, UInt(8.W))
    loadMemoryFromFile(mem, "src/hex/vse32_m2.hex")
    io.imem.inst := cat(
        mem(io.imem.addr + 3.U(WORD_LEN.W)),
        mem(io.imem.addr + 2.U(WORD_LEN.W)),
        mem(io.imem.addr + 1.U(WORD_LEN.W)),
        mem(io.imem.addr)
    )

    def readData(len: Int) = Cat(Seq.tabulate(len/8)(n => mem(io.dmem.addr + n.U(WORD_LEN.W))).reverse) // 追加
    io.dmem.rdata  := readData(WORD_LEN) // 追加
    io.dmem.vrdata := readData(VLEN*8) // 追加
    /*
    e.g.) readData(32)は次のように展開
    Cat(Seq.tabulate(4)(n => mem(io.dmem.addr + n.U(WORD_LEN.W))).reverse)
    ↓
    Cat(List(
        mem(io.dmem.addr + 0.U(WORD_LEN.W)),
        mem(io.dmem.addr + 1.U(WORD_LEN.W)),
        mem(io.dmem.addr + 2.U(WORD_LEN.W)),
        mem(io.dmem.addr + 3.U(WORD_LEN.W))
    ).reverse)
    ↓
    Cat(List(
        mem(io.dmem.addr + 3.U(WORD_LEN.W)),
        mem(io.dmem.addr + 2.U(WORD_LEN.W)),
        mem(io.dmem.addr + 1.U(WORD_LEN.W)),
        mem(io.dmem.addr + 0.U(WORD_LEN.W))
    ))
    */
}