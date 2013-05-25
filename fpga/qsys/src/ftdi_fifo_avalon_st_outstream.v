/*
 * module: ftdi_fifo_avalon_st_outstream
 * Date:2013/04/22
 * Author: Keisuke SUZUKI
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
module ftdi_fifo_avalon_st_outstream (
    // Connect to Inner Logic
    output          oST_READY,
    input           iST_VALID,
    input   [7:0]   iST_DATA,

    // Connect to FTDI FIFO Module
    input           iFIFO_TXE_n,    // Write to FIFO
    output          oFIFO_WR_n,     // Write Enable
    output  [7:0]   oFIFO_DATA,     // Write Data
    output          oFIFO_OE_n,     // Output Enable for Bi-direction Bus

    // Connect to System Signals
    input   clk,                    // System Clock xxMHz(xxns)
    input   rst                     // System Reset
);

//////////////////////////////////////////////////////////////////////////////
// parameter
//////////////////////////////////////////////////////////////////////////////
parameter   ST_IDLE                     = 3'b000,
            ST_AVALON_ST_ASSERT_READY   = 3'b001,
            ST_AVALON_ST_WAIT_VALID     = 3'b010,
            ST_WRITE_FIFO_START         = 3'b011,
            ST_WRITE_FIFO_WAIT_END      = 3'b100;

//////////////////////////////////////////////////////////////////////////////
// reg and wire
//////////////////////////////////////////////////////////////////////////////
reg [2:0]   state;              // State Machine state

// write inner ctrl signal
reg         wr_act_n;           // Activate write sequence
wire        wr_done_n;          // Done write sequence
wire        wr_ready_n;         // Ready write sequence

// write fifo ctrl signal
wire        fifo_txe_n;         // TXE signal from FIFO
wire        fifo_wr_n;          // WR signal to FIFO
wire[7:0]   fifo_wr_data;       // Data to FIFO
wire        fifo_oe_n;          // Output Enable for Bi-direction Bus

// AvalonST control signal
reg         st_ready;           // AvalonST ready signal
wire        st_valid;           // AvalonST valid signal
wire[7:0]   st_data;            // AvalonST data bus
reg [7:0]   st_data_reg;        // AvalonST data bus register

//////////////////////////////////////////////////////////////////////////////
// instance
//////////////////////////////////////////////////////////////////////////////
ftdi_fifo_wr ftdi_fifo_wr(
    // Connect to Inner Logic
    .iACT_WR_n(wr_act_n),
    .oDONE_WR_n(wr_done_n),
    .oREADY_WR_n(wr_ready_n),
    .iWR_DATA(st_data_reg),
    
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
assign oST_READY    = st_ready;
assign st_valid     = iST_VALID;
assign st_data      = iST_DATA;

assign fifo_txe_n   = iFIFO_TXE_n;
assign oFIFO_WR_n   = fifo_wr_n;
assign oFIFO_DATA   = fifo_wr_data;
assign oFIFO_OE_n   = fifo_oe_n;

// state machine
always@(posedge clk or negedge rst)
    if(!rst) begin
        wr_act_n        <= 1'b1;
        st_data_reg     <= 0;
        st_ready        <= 1'b0;
        state           <= ST_IDLE;
    end else begin
        case(state)
        ST_IDLE: begin
            wr_act_n        <= 1'b1;
            st_data_reg     <= 0;
            st_ready        <= 1'b0;
            state           <= ST_AVALON_ST_ASSERT_READY;
        end

        // Ready data from Avalon-ST
        ST_AVALON_ST_ASSERT_READY: begin
            wr_act_n        <= 1'b1;
            st_data_reg     <= st_data;
            st_ready        <= 1'b1;
            state           <= ST_AVALON_ST_WAIT_VALID;
        end

        // Wait valid signal from Avalon-ST
        ST_AVALON_ST_WAIT_VALID: begin
            wr_act_n        <= 1'b1;
            st_data_reg     <= st_data;
            if(st_valid == 1'b1) begin
                st_ready        <= 1'b0;
                state           <= ST_WRITE_FIFO_START;
            end else begin
                st_ready        <= 1'b1;
                state           <= ST_AVALON_ST_WAIT_VALID;
            end
        end

        // Activate ftdi_fifo_wr module's act signal
        ST_WRITE_FIFO_START: begin
            wr_act_n        <= 1'b0;
            st_data_reg     <= st_data_reg;
            st_ready        <= 1'b0;
            state           <= ST_WRITE_FIFO_WAIT_END;
        end

        // Wait ftdi_fifo_wr module's done signal
        ST_WRITE_FIFO_WAIT_END: begin
            wr_act_n        <= 1'b1;
            st_data_reg     <= st_data_reg;
            st_ready        <= 1'b0;
            if(wr_done_n == 1'b0) begin
                state           <= ST_WRITE_FIFO_WAIT_END;
            end else begin
                state           <= ST_AVALON_ST_ASSERT_READY;
            end
        end

        endcase
    end // end of if(!rst) else
endmodule
