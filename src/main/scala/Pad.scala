
import chisel3._
import chisel3.util._
class Pad extends Module {

  val io = IO(new Bundle {
    val in = Flipped(DecoupledIO(Vec(64, UInt(8.W))))
    val last_byte_index = Input(UInt(7.W)) // range 0-64

    val out = DecoupledIO(Vec(64, UInt(8.W)))

  })

  val sReady :: sTransmit :: sPadding :: sPaddingLengthNext :: sPadding1andLengthNext :: sStateDelay :: Nil = Enum(6)
  val state = RegInit(sReady)




  val byteCounter = RegInit(0.U(32.W))
  val theLastBlock = Reg(Vec(64, UInt(8.W)))
  val outvalid = RegInit(false.B)

  outvalid := false.B
  io.in.ready := false.B
  io.out.valid := outvalid

// if you want to set width of "length", match Bytecounter's
  val length = byteCounter*8.U
  io.out.bits := theLastBlock
  printf("out.valid: %d, %d %x\n", io.out.valid.asUInt, io.last_byte_index,  theLastBlock(60))
//  printf(" out: %x\n", io.out.bits(1))

  switch(state){

    is(sReady){
      io.in.ready := true.B
      when(io.in.valid){
        theLastBlock := io.in.bits
        state := sTransmit
      }
    }

    is(sTransmit){
      when(io.last_byte_index >= 64.U){ // not the last block
        when (io.out.ready) {
          io.out.valid := true.B
        }
          byteCounter := byteCounter + 64.U

      }.elsewhen(io.last_byte_index > 54.U && io.last_byte_index <= 62.U){

        byteCounter := byteCounter + io.last_byte_index +1.U
        theLastBlock(io.last_byte_index+1.U):= 0x80.U
        when (io.out.ready) {
          //outvalid delay
          outvalid := true.B
        }
        state := sPaddingLengthNext

      }.elsewhen(io.last_byte_index === 63.U){
        byteCounter := byteCounter + io.last_byte_index +1.U
        when (io.out.ready) {
          outvalid := true.B
          // // this is okay because update of output.bits has been done last cycle
        }
        state := sPadding1andLengthNext
      }.otherwise{ // general situation
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
        //not synchronized, advance one cycle?
        state := sStateDelay
    }

    is(sPaddingLengthNext){

      for(i <- 0 to 59 ){
        theLastBlock(i) := 0.U
      }
      theLastBlock(63) := length(7, 0)
      theLastBlock(62) := length(15, 8)
      theLastBlock(61) := length(23, 16)
      theLastBlock(60) := length(31, 17)
      state := sStateDelay
    }

    is(sPadding1andLengthNext){
      for(i <- 1 to 59 ){
        theLastBlock(i) := 0.U
      }
      theLastBlock(0) := 0x80.U
      theLastBlock(63) := length(7, 0)
      theLastBlock(62) := length(15, 8)
      theLastBlock(61) := length(23, 16)
      theLastBlock(60) := length(31, 17)
      state := sStateDelay

    }

    is(sStateDelay){
      when(io.out.ready){
        io.out.valid := true.B
        state := sReady
      }
    }

  }
//  printf("%x %x %x", theLastBlock(0), theLastBlock(1),theLastBlock(63))
//  printf("%d %b\n", byteCounter, length)
}