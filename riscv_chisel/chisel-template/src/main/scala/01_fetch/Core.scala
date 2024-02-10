packeage fetch

import chisel3._
import chisel3.util._
import common.Consts._

class Core extends Module {
    val io = IO(new Bundle {
        /* ImemPortIoをインスタンス化したものをFlippedで反転
        つまり、出力ポートaddr, 入力ポートinstを生成*/
        val imem = Flipped(new ImemPortIo())

        // 出力ポートexitはプログラム処理が終わった際にtrue.Bとなる
        val exit = Output(Bool())
    })

    /* 32bit幅*32本のレジスタを生成
    WORD_LEN=32(Consts.scalaで定義) */
    val regfile = Mem(32, Uint(WORD_LEN.W))

    // ******
    // Instruction Fetch (IF) Stage

    /* 初期値を0とするレジスタ生成
    サイクルごとに4ずつカウントアップ */
    val pc_reg = RegInit(START_ADDR)
    pc_reg := pc_reg + 4.U(WORD_LEN.W)

    // 出力ポートaddrにpc_regで接続し、入力ポートinstをinstで受ける
    io.imem.addr := pc_reg
    val inst = io.imem.inst

    // exit信号はinstが34333231(読み込ませるプログラムの最終行)の場合にtrue.Bとする
    // (プログラムの内容は後述)
    io.exit := (inst == 0x34333231.U(WORD_LEN.W))
}