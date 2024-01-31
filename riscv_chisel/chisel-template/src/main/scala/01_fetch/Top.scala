// CPUとメモリを接続するラッパー
package fetch

import chisel3._
import chisel3.util._

class Top extends Module {
    val io = IO(new Bundle{
        val exit = Output(Bool())
    })

    // CoreクラスとMemoryクラスをnewでインスタンス化、Moduleでハードウェア化
    val core = Module(new Core())
    val memory = Module(new Memory())

    // coreのioとmemoryのioはImemPortIoを反転した関係にあるので"<>"で一括接続
    core.io.imem <> memory.io.imem

    io.exit := core.io.exit
}