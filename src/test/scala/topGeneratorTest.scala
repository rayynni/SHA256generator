import chisel3._
import chisel3.util._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class topGeneratorTest extends AnyFlatSpec with ChiselScalatestTester{
  behavior of "sha256 generator"

  it should "work well" in {
    test(new topGenerator){dut=>

      dut.io.in.valid.poke(true.B)
      dut.io.out.ready.poke(true.B)
      dut.io.in.bits(0).poke(0xff.U)
      for (i <- 1 to  63) {
        dut.io.in.bits(i).poke(0.U)
      }
      dut.io.last_byte_index.poke(0)
      dut.clock.step(3)
      while(!dut.io.out.valid.peek().litToBoolean){
        dut.clock.step()
      }

      println(cf"${dut.io.out.valid.peek().litToBoolean} ${dut.io.out.bits.peek().litValue}%x")
      dut.clock.step()
    }
  }
}
