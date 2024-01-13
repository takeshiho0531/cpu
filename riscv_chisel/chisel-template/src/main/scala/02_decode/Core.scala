// レジスタ番号の解読
val rs1_addr = inst(19, 15)  // rs1レジスタ番号は命令列の15-19bit目
val rs2_addr = inst(24, 20) // rs2レジスタ番号は命令列の20-24bit目
val wb_addr = inst(11, 7) // rdレジスタ番号は命令reduceつの7-11bit目

// レジスタデータの読み出し
val rs1_data = Mux((rs1_addr =/= 0.U(WORD_LEN.U)), regfile(rs1_addr), 0.U(WORD_LEN.W))
val rs2_data = Mux((rs1_addr =/= 0.U(WORD_LEN.U)), regfile(rs2_addr), 0.U(WORD_LEN.W))

// デバッグ用信号を追加
printf(p"rs1_addr: $rs1_addr\n")
printf(p"rs2_addr: $rs2_addr\n")
printf(p"wb_addr: $wb_addr\n")
printf(p"rs1_addr: 0x${Hexadecimal(rs1_addr)}\n")
printf(p"rs2_addr: 0x${Hexadecimal(rs2_addr)}\n")