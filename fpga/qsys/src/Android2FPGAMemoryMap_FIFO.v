/*
 * Copyright (C) 2013 Keisuke SUZUKI
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

//////////////////////////////////////////////////////////////////////////////
// includes
//////////////////////////////////////////////////////////////////////////////
`include "timescale.v"

//////////////////////////////////////////////////////////////////////////////
// module and I/O ports
//////////////////////////////////////////////////////////////////////////////
module Android2FPGAMemoryMap_FIFO (
    // Connect to FTDI FIFO Module
    input           iFIFO_RXF_n,    // Read from FIFO
    output          oFIFO_RD_n,     // Read Enable
    input   [7:0]   iFIFO_DATA,     // Read Data

    input           iFIFO_TXE_n,    // Write to FIFO
    output          oFIFO_WR_n,     // Write Enable
    output  [7:0]   oFIFO_DATA,     // Write Data
    output          oFIFO_OE_n,     // Output Enable for Bi-direction Bus

    output          oSIWU_n,        // Send Immidiate / Wake Up signal

    output  [7:0]   oPIO,           // Output PIO

    // Connect to System Signals
    input   clk,                    // System Clock 50MHz(20ns)
    input   rst                     // System Reset
);

//////////////////////////////////////////////////////////////////////////////
// parameter
//////////////////////////////////////////////////////////////////////////////

//////////////////////////////////////////////////////////////////////////////
// reg and wire
//////////////////////////////////////////////////////////////////////////////
// Avalon-ST instream
wire        st_in_stream_ready;
wire        st_in_stream_valid;
wire [7:0]  st_in_stream_data;

// Avalon-ST outstream
wire        st_out_stream_ready;
wire        st_out_stream_valid;
wire [7:0]  st_out_stream_data;

// read fifo ctrl signal
wire        fifo_rxf_n;         // RXF signal from FIFO
wire        fifo_rd_n;          // RD signal to FIFO
wire [7:0]  fifo_rd_data;       // Data from FIFO

// write fifo ctrl signal
wire        fifo_txe_n;         // TXE signal from FIFO
wire        fifo_wr_n;          // WR signal to FIFO
wire [7:0]  fifo_wr_data;       // Data to FIFO
wire        fifo_oe_n;          // Output Enable for Bi-direction Bus

wire [7:0]  pio;

//////////////////////////////////////////////////////////////////////////////
// RTL instance
//////////////////////////////////////////////////////////////////////////////

Android2FPGAMemoryMap u0 (
    .clk_clk                                    (clk),                  //                                  clk.clk
    .reset_reset_n                              (rst),                  //                                reset.reset_n
    .st_bytes_to_packets_in_bytes_stream_ready  (st_in_stream_ready),   //  st_bytes_to_packets_in_bytes_stream.ready
    .st_bytes_to_packets_in_bytes_stream_valid  (st_in_stream_valid),   //                                     .valid
    .st_bytes_to_packets_in_bytes_stream_data   (st_in_stream_data),    //                                     .data
    .st_packets_to_bytes_out_bytes_stream_ready (st_out_stream_ready),  // st_packets_to_bytes_out_bytes_stream.ready
    .st_packets_to_bytes_out_bytes_stream_valid (st_out_stream_valid),  //                                     .valid
    .st_packets_to_bytes_out_bytes_stream_data  (st_out_stream_data),   //                                     .data
    .pio_export                                 (pio)                   //                                  pio.export
);

ftdi_fifo_avalon_st_instream ftdi_fifo_avalon_st_instream(
    // Connect to Inner Logic
    .iST_READY(st_in_stream_ready),
    .oST_VALID(st_in_stream_valid),
    .oST_DATA(st_in_stream_data),

    // Connect to FTDI FIFO Module
    .iFIFO_RXF_n(fifo_rxf_n),
    .oFIFO_RD_n(fifo_rd_n),
    .iFIFO_DATA(fifo_rd_data),

    // Connect to System Signals
    .clk(clk),
    .rst(rst)
);

ftdi_fifo_avalon_st_outstream ftdi_fifo_avalon_st_outstream(
    // Connect to Inner Logic
    .oST_READY(st_out_stream_ready),
    .iST_VALID(st_out_stream_valid),
    .iST_DATA(st_out_stream_data),

    // Connect to FTDI FIFO Module
    .iFIFO_TXE_n(fifo_txe_n),
    .oFIFO_WR_n(fifo_wr_n),
    .oFIFO_DATA(fifo_wr_data),
    .oFIFO_OE_n(fifo_oe_n),

    // Connect to System Signals
    .clk(clk),
    .rst(rst)
);

//////////////////////////////////////////////////////////////////////////////
// RTL
//////////////////////////////////////////////////////////////////////////////
assign fifo_rxf_n   = iFIFO_RXF_n;
assign oFIFO_RD_n   = fifo_rd_n;
assign fifo_rd_data = iFIFO_DATA;

assign fifo_txe_n   = iFIFO_TXE_n;
assign oFIFO_WR_n   = fifo_wr_n;
assign oFIFO_DATA   = fifo_wr_data;
assign oFIFO_OE_n   = fifo_oe_n;

assign oSIWU_n      = 1'b0;

assign oPIO         = pio;

endmodule
