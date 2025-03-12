import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec


class inputManagerTest extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "inputManager"

  it should "handle short message" in {
    test(new inputManager) { dut =>

      val messageBlock = Vec(64, UInt(8.W))
      dut.io.in.bits(0).poke(0xff.U)
      dut.io.in.bits(1).poke(0xff.U)
      for(i<-2 to 63){
        dut.io.in.bits(i).poke(0.U)
      }

      dut.io.in.valid.poke(true.B)
      dut.io.out.ready.poke(true.B)
      dut.io.last_byte_index.poke(1.U)

      while(!dut.io.out.valid.peek().litToBoolean){
        dut.clock.step(1)
        println(s"Valid: ${dut.io.out.valid.peek().litToBoolean}")
        println(s"index: ${dut.io.last_byte_index.peek()}")
        println(s"Output[0]: ${dut.io.out.bits(0).peek()}")
        println(s"Output[1]: ${dut.io.out.bits(1).peek()}")
        println(s"Output[2]: ${dut.io.out.bits(2).peek()}")
        println(s"Output[63]: ${dut.io.out.bits(63).peek()}")
      }
      dut.clock.step(1) // why???
      dut.io.out.bits(2).expect(0x80.U)
      dut.io.out.bits(63).expect(0x10.U)

    }
  }

  it should "handle more than one block message" in {
    test(new inputManager) { dut =>

      val messageBlock = Vec(64, UInt(8.W))
      dut.io.in.bits(0).poke(0xff.U)
      dut.io.in.bits(1).poke(0x80.U)
      for (i <- 2 until  63) {
        dut.io.in.bits(i).poke(0.U)
      }

      dut.io.in.valid.poke(true.B)
      dut.io.out.ready.poke(true.B)

      step(1)
      dut.io.in.valid.poke(false.B)

      while (!dut.io.out.valid.peek().litToBoolean) {
        dut.clock.step(1)
        println(s"Valid: ${dut.io.out.valid.peek().litToBoolean}")
        println(s"index: ${dut.io.last_byte_index.peek()}")
        println(s"Output[0]: ${dut.io.out.bits(0).peek()}")
        println(s"Output[1]: ${dut.io.out.bits(1).peek()}")
        println(s"Output[2]: ${dut.io.out.bits(2).peek()}")
        println(s"Output[63]: ${dut.io.out.bits(63).peek()}")
      }
      dut.clock.step(1) // why???

      dut.io.in.valid.poke(true.B)
      dut.io.in.bits(0).poke(0xff.U)
      dut.io.in.bits(1).poke(0x80.U)
      dut.io.in.bits(2).poke(0xaa.U)
      dut.io.last_byte_index.poke(2.U)

      while (!dut.io.out.valid.peek().litToBoolean) {
        dut.clock.step(1)
        println(s"Valid: ${dut.io.out.valid.peek().litToBoolean}")
        println(s"index: ${dut.io.last_byte_index.peek()}")
        println(s"Output[0]: ${dut.io.out.bits(0).peek()}")
        println(s"Output[1]: ${dut.io.out.bits(1).peek()}")
        println(s"Output[2]: ${dut.io.out.bits(2).peek()}")
        println(s"Output[63]: ${dut.io.out.bits(63).peek()}")
      }
      dut.clock.step(1)

      dut.io.out.bits(3).expect(0x80.U)
      dut.io.out.bits(62).expect(0x02.U)
      dut.io.out.bits(63).expect(0x18.U)

    }
  }

  it should "handle edge situation" in {
    test(new inputManager) { dut =>

      val messageBlock = Vec(64, UInt(8.W))
      dut.io.in.bits(0).poke(0xff.U)
      dut.io.in.bits(1).poke(0xff.U)
      for(i<-2 until  63){
        dut.io.in.bits(i).poke(0.U)
      }
      dut.io.in.bits(63).poke(0xff.U)

      dut.io.in.valid.poke(true.B)
      dut.io.out.ready.poke(true.B)
      dut.io.last_byte_index.poke(63.U)

      while(!dut.io.out.valid.peek().litToBoolean){
        dut.clock.step(1)
        println(s"Valid: ${dut.io.out.valid.peek().litToBoolean}")
        println(s"index: ${dut.io.last_byte_index.peek()}")
        println(s"Output[0]: ${dut.io.out.bits(0).peek()}")
        println(s"Output[1]: ${dut.io.out.bits(1).peek()}")
        println(s"Output[2]: ${dut.io.out.bits(2).peek()}")
        println(s"Output[63]: ${dut.io.out.bits(63).peek()}")
      }
      dut.io.out.ready.poke(false.B)

      dut.clock.step(5)
      dut.io.out.ready.poke(true.B)
      dut.clock.step(1)

      dut.io.out.bits(0).expect(0x80.U)
      dut.io.out.bits(62).expect(0x02.U)

    }
  }

}