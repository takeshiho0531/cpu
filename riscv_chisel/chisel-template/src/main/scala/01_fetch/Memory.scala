package fetch

import chisel3._
import chisel3.util._
import chisel3.util.experimantal.loadMemoryFromFile
import common.Consts._

/* ImemPortIoクラスはbundleを継承する形でaddrとinstをまとめる
addr: メモリアドレス用の入力ポート
inst: 命令データ用の出力ポート
ともに32bit幅 (なぜならWORD_LEN=32) */
class ImemPortIo extends Bundle {
    val addr = Input(UInt(WORD_LEN.W))
    val inst = Output(UInt(WORD_LEN.W))
}

class Meomry extends Module {
    val io = IO(new Bundle {
        val imem = new ImemPortIo()
    })

    /* メモリの実体として、8bit幅*16384本(16KB)のレジスタを生成。
    8bit幅である理由は、PCのカウントアップ幅を4にするため。
    1アドレスに8bit、つまり4アドレスに32bit格納する形 */

    val mem = Mem(16384, UInt(8.W))

    // メオm裏データロード(hexファイル内容は後述)
    loadMemoryFromFile(mem, "src/hex/fetch.hex")

    // 各アドレスに格納された8bitのデータを4つ繋げて32bitデータに
    io.imem.inst := Cat(
        mem(io.imem.addr + 3.U(WORD_LEN.W)),
        mem(io.imem.addr + 2.U(WORD_LEN.W)),
        mem(io.imem.addr + 1.U(WORD_LEN.W),
        mem(io.imem.addr))
    )
}