import chisel3._
import circt.stage._
import firrtl.annotations._
import firrtl.options._

object Main extends App {
  ChiselStage.emitSystemVerilogFile(
      new topGenerator,
    firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info")
  )

}
