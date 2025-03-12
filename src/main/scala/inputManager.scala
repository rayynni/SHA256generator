import chisel3._
import chisel3.util._
class inputManager extends Module {

  val io = IO(new Bundle {
    val in = Flipped(DecoupledIO(Vec(64, UInt(8.W))))
    val last_byte_index = Input(UInt(6.W)) // range 0-63

    val out = DecoupledIO(Vec(64, UInt(8.W)))
    val out_last = Output(Bool())
  })

  val sReady :: sTransmit :: sPadding :: sPaddingLengthNext :: sPadding1andLengthNext :: Nil = Enum(5)
  val state = RegInit(sReady)

  io.in.ready := false.B
  io.out.valid := false.B
  io.out_last := false.B

  val byteCounter = RegInit(0.U(32.W))
  val theLastBlock = Reg(Vec(64, UInt(8.W)))
  val length = byteCounter*8.U

  theLastBlock := io.in.bits
  io.out.bits := theLastBlock

  switch(state){
    is(sReady){
      io.in.ready := true.B
      when(io.in.valid){
        state := sTransmit
      }
    }

    is(sTransmit){
      when(io.last_byte_index === 0.U){ // not the last block
        when (io.out.ready) {
          io.out.valid := true.B
        }
        byteCounter := byteCounter + 64.U
      }.elsewhen(io.last_byte_index > 54.U && io.last_byte_index <= 62.U){
          byteCounter := byteCounter + io.last_byte_index +1.U
          theLastBlock(io.last_byte_index+1.U):= 0x80.U
        when (io.out.ready) {
          io.out.valid := true.B
        }
        state := sPaddingLengthNext
        }.elsewhen(io.last_byte_index === 63.U){
          byteCounter := byteCounter + io.last_byte_index +1.U
        when (io.out.ready) {
          io.out.valid := true.B
        }
        state := sPadding1andLengthNext
        }.otherwise{
        state := sPadding
        byteCounter := byteCounter + io.last_byte_index +1.U
      }
    }

    is(sPadding){
       // the last block has enough space
        theLastBlock(io.last_byte_index+1.U):= 0x80.U
        theLastBlock(63) := length(7, 0)
        theLastBlock(62) := length(15, 8)
        theLastBlock(61) := length(23, 16)
        theLastBlock(60) := length(31, 17)
        io.out.valid := true.B
        io.out_last := true.B
        when(io.out.ready){state := sReady}
    }

    is(sPaddingLengthNext){
      theLastBlock(63) := length(7, 0)
      theLastBlock(62) := length(15, 8)
      theLastBlock(61) := length(23, 16)
      theLastBlock(60) := length(31, 17)
      io.out.valid := true.B
      io.out_last := true.B
      when(io.out.ready){state := sReady}
    }

    is(sPadding1andLengthNext){
      theLastBlock(0) := 0x80.U
      theLastBlock(63) := length(7, 0)
      theLastBlock(62) := length(15, 8)
      theLastBlock(61) := length(23, 16)
      theLastBlock(60) := length(31, 17)
      io.out.valid := true.B
      io.out_last := true.B
      when(io.out.ready){state := sReady}
    }

  }

}