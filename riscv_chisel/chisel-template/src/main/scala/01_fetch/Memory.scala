package fetch

import chisel3._
import chisel3.util._
import chisel3.util.experimental.loadMemoryFromFile
import common.Consts._

/* ImemPortIoクラスはBundleを継承する形で、addrとinstの2信号をまとめている。
addr: メモリアドレス用の入力ポート
inst: 命令データ用の出力ポート
ともに32bit幅(理由: WORD_LEN=32) */

class ImemPortIo extends Bundle {
    val addr = Input(Uint(WORD_LEN.W))
    val inst = Output(Uint(WORD_LEN.W))
}

class Memory extends Module {
    val io = IO(new Bundle {
        val imem = new ImemPortIo()
    })

    /* メモリの実体として、8bit幅*16384本(16KB)のレジスタを生成する。
    8bit幅である理由は、pcのカウントアップ幅を4にするため。
    1アドレスに8bit, つまり4アドレスに32bit格納する形になる */
    val mem = Mem(16384, UInt(8.W))

    // メモリデータをロード(hexファイル内容は後述)
    loadMemoryFromFile(mem, "src/hex/fetch.hex")

    // 各アドレスに格納された8bitデータを4つ繋げて32bitデータに
    io.imem.inst := Cat(
        mem(io.imem.addr + 3.U(WORD_LEN.W)),
        mem(io.imem.addr + 2.U(WORD_LEN.W)),
        mem(io.imem.addr + 2.U(WORD_LEN.W)),
        mem(io.imem.addr),
    )
}