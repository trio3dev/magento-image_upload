package com.dakidev.image_upload.domain;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Map;

public class UpdateExcelFileService {
    
    public void updateColumnInExcelFile(File destinationFile, int columnIndex, Map<Integer, String> newColumnValues) throws IOException {
        
        if(newColumnValues.isEmpty())
            return;
        
        try (
                FileInputStream inputStream = new FileInputStream(destinationFile);
                Workbook workbook = new XSSFWorkbook(inputStream);
                OutputStream outputStream = Files.newOutputStream(destinationFile.toPath())) {
            
            Sheet sheet = workbook.getSheetAt(0);
            
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                
                String value = newColumnValues.get(i);
                
                if (sheet.getRow(i) != null && value != null) {
                    updateCellValue(sheet.getRow(i), value, columnIndex);
                }
            }
            
            workbook.write(outputStream);
        }
    }
    
    private void updateCellValue(Row currentRow, String value, int ColumnIndex) {
        
        currentRow.createCell(ColumnIndex).setCellValue(value);
    }
    
}
