import chisel3._
import chisel3.util._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class WcalculatorTest extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "Wcalculator"

  it should "calculate value W " in {
    test(new Wcalculator) { dut =>
      var clock = 0
      dut.clock.setTimeout(50)
      dut.io.in.bits(0).poke(0xff800000L.U) //Long!!
      dut.io.in.bits(15).poke(0x00000008L.U)
      for(i<-1 to 14){
        dut.io.in.bits(i).poke(0x0.U)
      }
      dut.io.in.valid.poke(true.B)
      dut.clock.step(1)
      dut.io.in.valid.poke(false.B)
      dut.io.out.ready.poke(true.B)

      while(!dut.io.out.valid.peek().litToBoolean){
        clock = clock+1
        println(s"CLOCK: $clock")
        dut.clock.step(1)
//        for(i<-0 to 20){
//          println(i + s": ${dut.io.out.bits(i).peek()}")
//        }

        println(s"Valid: ${dut.io.out.valid.peek().litToBoolean}")
        println(s"Output[63]: ${dut.io.out.bits(62).peek()}")
        println(s"Output[63]: ${dut.io.out.bits(63).peek()}")
      }
      dut.clock.step(1)
      dut.io.out.ready.poke(false.B)
      dut.io.out.bits(62).expect(0xC8FD7F50L.U)
      dut.io.out.bits(63).expect(0xCB9DC26DL.U)

    }
  }
}