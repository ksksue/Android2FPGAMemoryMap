/*
 * module: ftdi_fifo_avalon_st_instream
 * Date:2013/04/22
 * Author: keisuke
 * Description
 *  
 */

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
module ftdi_fifo_avalon_st_instream (
    // Connect to Inner Logic
    input           iST_READY,
    output          oST_VALID,
    output  [7:0]   oST_DATA,

    // Connect to FTDI FIFO Module
    input           iFIFO_RXF_n,
    output          oFIFO_RD_n,
    input   [7:0]   iFIFO_DATA,

    // Connect to System Signals
    input   clk,                    // System Clock 50MHz(20ns)
    input   rst                     // System Reset
);

//////////////////////////////////////////////////////////////////////////////
// parameter
//////////////////////////////////////////////////////////////////////////////
parameter   ST_IDLE                     = 3'b000,
            ST_FIFO_READ_START          = 3'b001,
            ST_FIFO_READ_WAIT_END       = 3'b010,
            ST_AVALON_ST_WAIT_READY     = 3'b011,
            ST_AVALON_ST_ASSERT_VALID   = 3'b100;

//////////////////////////////////////////////////////////////////////////////
// reg and wire
//////////////////////////////////////////////////////////////////////////////
reg [2:0]   state;              // State Machine state

// read inner ctrl signal
reg         rd_act_n;           // Avtivate read sequence
wire        rd_done_n;          // Done read sequence
wire        rd_ready_n;         // Ready read sequence
reg  [7:0]  rd_data;            // Read data
wire [7:0]  rd_data_from_fifo;  // Read data from FIFO

// read fifo ctrl signal
wire        fifo_rxf_n;         // RXF signal from FIFO
wire        fifo_rd_n;          // RD signal to FIFO
wire [7:0]  fifo_rd_data;       // Data from FIFO

// AvalonST control signal
wire        st_ready;           // AvalonST ready signal
reg         st_valid;           // AvalonST valid signal
//reg [7:0]   st_data;            // AvalonST data bus // use rd_data

//////////////////////////////////////////////////////////////////////////////
// instance
//////////////////////////////////////////////////////////////////////////////
ftdi_fifo_rd ftdi_fifo_rd(
    // Connect to Inner Logic
    .iACT_RD_n(rd_act_n),
    .oDONE_RD_n(rd_done_n),
    .oREADY_RD_n(rd_ready_n),
    .oRD_DATA(rd_data_from_fifo),

    // Connect to FTDI FIFO Module
    .iFIFO_RXF_n(fifo_rxf_n),
    .oFIFO_RD_n(fifo_rd_n),
    .iFIFO_DATA(fifo_rd_data),

    // Connect to System Signals
    .clk(clk),
    .rst(rst)
);
//////////////////////////////////////////////////////////////////////////////
// RTL
//////////////////////////////////////////////////////////////////////////////

assign st_ready     = iST_READY;
assign oST_VALID    = st_valid;
assign oST_DATA     = rd_data;

assign fifo_rxf_n   = iFIFO_RXF_n;
assign oFIFO_RD_n   = fifo_rd_n;
assign fifo_rd_data = iFIFO_DATA;

// state machine
always@(posedge clk or negedge rst)
    if(!rst) begin
        rd_act_n    <= 1'b1;
        rd_data     <= 0;
        st_valid    <= 1'b0;
        state       <= ST_IDLE;
    end else begin
        case(state)
        ST_IDLE: begin
            rd_act_n    <= 1'b1;
            rd_data     <= 0;
            st_valid    <= 1'b0;
            state       <= ST_FIFO_READ_START;
        end

        // Activate ftdi_fifo_rd module's act signal
        ST_FIFO_READ_START: begin
            rd_act_n    <= 1'b0;
            rd_data     <= rd_data;
            st_valid    <= 1'b0;
            state       <= ST_FIFO_READ_WAIT_END;
        end

        // Wait ftdi_fifo_rd module's done signal
        ST_FIFO_READ_WAIT_END: begin
            rd_act_n    <= 1'b1;
            rd_data     <= rd_data_from_fifo;
            st_valid    <= 1'b0;
            if(rd_done_n == 1'b0) begin
                state   <= ST_AVALON_ST_WAIT_READY;
            end else begin
                state   <= ST_FIFO_READ_WAIT_END;
            end
        end

        // Wait avalon-st ready signal
        ST_AVALON_ST_WAIT_READY: begin
            rd_act_n    <= 1'b1;
            rd_data     <= rd_data;
            st_valid    <= 1'b0;
            if(st_ready == 1'b1) begin
                state   <= ST_AVALON_ST_ASSERT_VALID;
            end else begin
                state   <= ST_AVALON_ST_WAIT_READY;
            end
        end

        // Assert avalon-st valid signal
        ST_AVALON_ST_ASSERT_VALID: begin
            rd_act_n    <= 1'b1;
            rd_data     <= rd_data;
            st_valid    <= 1'b1;
            state       <= ST_FIFO_READ_START; // deassert valid signal on ST_FIFO_READ_START
        end

        endcase
    end // end of if(!rst) else

endmodule
