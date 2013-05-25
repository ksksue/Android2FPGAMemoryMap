/*
 * Copyright(C) 2013 Keisuke SUZUKI
 * Licensed under the Apache License, Version 2.0
 * http ://www.apache.org/licenses/LICENSE-2.0
 */
//////////////////////////////////////////////////////////////////////////////
// includes
//////////////////////////////////////////////////////////////////////////////
`include "timescale.v"

//////////////////////////////////////////////////////////////////////////////
// module and I/O ports
//////////////////////////////////////////////////////////////////////////////
module DE0_NANO(
    //////////// LED //////////
    output  [7:0]   oLED,

    //////////// KEY //////////
    input   [1:0]  iKEY,

    //////////// SW //////////
    input   [3:0]   iSW,

    //////////// SDRAM //////////
    output  [12:0]  oDRAM_ADDR,
    output  [1:0]   oDRAM_BA,
    output          oDRAM_CAS_N,
    output          oDRAM_CKE,
    output          oDRAM_CLK,
    output          oDRAM_CS_N,
    inout   [15:0]  ioDRAM_DQ,
    output  [1:0]   oDRAM_DQM,
    output          oDRAM_RAS_N,
    output          oDRAM_WE_N,

    //////////// EPCS //////////
    output          oEPCS_ASDO,
    input           iEPCS_DATA0,
    output          oEPCS_DCLK,
    output          oEPCS_NCSO,

    //////////// Accelerometer and EEPROM //////////
    output          oG_SENSOR_CS_N,
    input           iG_SENSOR_INT,
    output          oI2C_SCLK,
    inout           ioI2C_SDAT,

    //////////// ADC //////////
    output          oADC_CS_N,
    output          oADC_SADDR,
    output          oADC_SCLK,
    input           iADC_SDAT,

    //////////// 2x13 GPIO Header //////////
//  inout   [12:0]  GPIO_2;
//  input   [2:0]   GPIO_2_IN;

    //////////// GPIO_0, GPIO_0 connect to GPIO Default //////////
//  inout   [33:0]  GPIO_0_D;
//  input   [1:0]   GPIO_0_IN;

    //////////// GPIO_0, GPIO_1 connect to GPIO Default //////////
//  inout   [33:0]  GPIO_1_D;
//  input   [1:0]   GPIO_1_IN;

    input           iFIFO_RXF_n,
    input           iFIFO_TXE_n,
    output  reg     oFIFO_RD_n,
    output  reg     oFIFO_WR_n,
    inout   [7:0]   ioFIFO_DATA,
    output          oFIFO_SIWU_n,

    //////////// CLOCK //////////
    input           iCLOCK_50
);

//////////////////////////////////////////////////////////////////////////////
// parameter
//////////////////////////////////////////////////////////////////////////////

//////////////////////////////////////////////////////////////////////////////
// reg and wire
//////////////////////////////////////////////////////////////////////////////
wire        clk;
wire        rst;

wire[7:0]   fifo_rd_data;   // read data from fifo
wire        fifo_rd_n;      // rd to fifo
reg         fifo_rxf_n;     // rxf from fifo

wire[7:0]   fifo_wr_data;   // write data to fifo
wire        fifo_wr_n;      // wr to fifo
reg         fifo_txe_n;     // txe from fifo
wire        fifo_oe_n;      // output enable for inout buf

wire        fifo_siwu_n;    // Send Immidiate / Wake Up signal

wire[7:0]   pio_data;       // Parallel IO Data


//////////////////////////////////////////////////////////////////////////////
// RTL instance
//////////////////////////////////////////////////////////////////////////////

//////////////////////////////////////////////////////////////////////////////
// RTL
//////////////////////////////////////////////////////////////////////////////

assign clk = iCLOCK_50;
assign rst = iKEY[0];
assign oLED = pio_data;
assign oFIFO_SIWU_n = fifo_siwu_n;

Android2FPGAMemoryMap_FIFO Android2FPGAMemoryMap_FIFO(
    // Connect to FTDI FIFO Module
    .iFIFO_RXF_n(fifo_rxf_n),
    .oFIFO_RD_n(fifo_rd_n), 
    .iFIFO_DATA(fifo_rd_data),

    .iFIFO_TXE_n(fifo_txe_n),
//    .iFIFO_TXE_n(1'b1),         // TODO
    .oFIFO_WR_n(fifo_wr_n),
    .oFIFO_DATA(fifo_wr_data),
    .oFIFO_OE_n(fifo_oe_n),

    .oSIWU_n(fifo_siwu_n),

    .oPIO(pio_data),

    // Connect to System Signals
    .clk(clk),
    .rst(rst)
    );

always@(posedge clk) begin
    fifo_rxf_n <= iFIFO_RXF_n;
    fifo_txe_n <= iFIFO_TXE_n;
end 

always@(posedge clk) begin
    oFIFO_RD_n <= fifo_rd_n;
    oFIFO_WR_n <= fifo_wr_n;
end

param_inout_buf #(.DATA_WIDTH(8))
    iobuf(.ioDATA(ioFIFO_DATA[7:0]),
    .iOE_n(fifo_oe_n),
    .iDATA(fifo_rd_data),
    .oDATA(fifo_wr_data),
    .clk(clk));

endmodule
