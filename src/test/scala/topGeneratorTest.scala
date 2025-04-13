import chisel3._
import chisel3.util._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class topGeneratorTest extends AnyFlatSpec with ChiselScalatestTester{
  behavior of "sha256 generator"

  it should "work well" in {
    test(new topGenerator){dut=>
      // Set initial values
      dut.io.in.valid.poke(true.B)
      dut.io.out.ready.poke(true.B)
      dut.io.in.bits.poke(0xff.U)
      dut.io.last.poke(true.B)

      dut.clock.step(1)

      dut.clock.step(1)

      dut.io.in.valid.poke(false.B)
      
      // Wait for valid output
      while(!dut.io.out.valid.peek().litToBoolean){
        dut.clock.step()
      }

      println(cf"Output value: ${dut.io.out.bits.peek().litValue}%x")

    }
  }
}
