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

  it should "handle edge situation" in {
    test(new topGenerator) {dut =>

      dut.io.in.bits.poke(0xff.U)
      dut.io.in.valid.poke(true.B)
      dut.io.out.ready.poke(true.B)
      while(!dut.io.in.ready.peek().litToBoolean){
        dut.clock.step()
      }

      var count = 1
      while(count<=58){
        count = count + 1
        dut.clock.step()
      }
      dut.io.last.poke(true.B)
      dut.clock.step()
      dut.io.in.valid.poke(false.B)
      while(!dut.io.out.valid.peek().litToBoolean){

        dut.clock.step()
      }

      println(cf"${dut.io.out.bits.peek().litValue}%x")

    }
  }
}
