package cmd;
//Rebirth of Souls - Character Class
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

public class Chara 
{
	public static int numSlots;
	private static int rowCnt=0;
	private static String[] charaIdsFromCsv, charaNamesFromCsv;
	private static byte[] getCharaPanelContents(String charaId) throws IOException
	{
		byte[] charaIdBytes = new byte[5];
		byte[] charaPanelContents = new byte[173], ichigo = new byte[173], placeholder = new byte[173];
		int pos=4, sectionSize=173*numSlots;
		File charaSelectVanilla = new File("./bak/CharaSelectVanilla.bin");
		RandomAccessFile raf = new RandomAccessFile(charaSelectVanilla,"r");
		while (pos<sectionSize)
		{
			raf.read(placeholder);
			pos+=173;
			System.arraycopy(placeholder, 0, charaIdBytes, 0, 5);
			String currCharaId = new String(charaIdBytes);
			if (currCharaId.equals(charaId)) System.arraycopy(placeholder, 0, charaPanelContents, 0, 173);
			else if (currCharaId.equals("pl000")) System.arraycopy(placeholder, 0, ichigo, 0, 173);
			raf.seek(pos);
		}
		//if character ID is not present, then make use of placeholder data from Ichigo
		if (charaPanelContents[0]==0)
		{
			System.arraycopy(charaId.getBytes(), 0, ichigo, 0, 5);
			System.arraycopy(ichigo, 0, charaPanelContents, 0, 173);
		}
		raf.close();
		return charaPanelContents;
	}
	public static int getSlotIdFromCharaName(String[] charaNames, String name)
	{
		int slotId=-1;
		//linear search (oh lord) if the character name is already in the roster
		for (int i=0; i<charaNames.length; i++)
			if (name.equals(charaNames[i])) slotId=i;
		return slotId;
	}
	public static int[] getNumPanelsPerRow(RandomAccessFile bin) throws IOException
	{
		int rowCnt=0;
		int[] rowPanels = new int[7];
		bin.seek(0);
		while (bin.getFilePointer()<bin.length())
		{
			byte input = bin.readByte();
			if (input=='j')
			{
				byte[] rowIdBytes = new byte[4];
				bin.read(rowIdBytes);
				String rowId = new String(rowIdBytes);
				//row indicator
				if (rowId.startsWith("_p_")) rowCnt++;
				//panel indicator
				else if (rowId.equals("_pan")) rowPanels[rowCnt-1]++;
			}
		}
		return rowPanels;
	}
	public static String getCharaIdFromName(String charaName)
	{
		String charaId=null;
		//oh wow, another linear search (please lord) is needed
		for (int i=0; i<charaNamesFromCsv.length; i++)
			if (charaName.equals(charaNamesFromCsv[i])) charaId = charaIdsFromCsv[i];
		return charaId;
	}
	public static String[] getCharaIds(RandomAccessFile bin) throws IOException
	{
		bin.seek(0);
		byte[] charaIdBytes = new byte[5];
		numSlots=LittleEndian.getInt(bin.readInt());
		int charaCnt=0, sectionSize=173*numSlots, pos=4;
		String[] charaIds = new String[numSlots];
		while (pos<sectionSize && charaCnt<numSlots)
		{
			bin.read(charaIdBytes);
			pos+=5;
			String charaId = new String(charaIdBytes);
			charaIds[charaCnt] = charaId;
			charaCnt++; 
			pos+=168; //rest of character slot info
			bin.seek(pos);
		}
		return charaIds;
	}
	public static String[] getCharaNames(String[] charaIds) throws IOException
	{
		String[] charaNames = new String[charaIds.length];
		if (rowCnt==0) //initialize static string arrays only once
		{
			File charaNamesCsv = new File("./csv/chara.csv");
			Scanner sc = new Scanner(charaNamesCsv);
			ArrayList<String> rows = new ArrayList<String>();
			String header = sc.nextLine();
			if (!header.equals("chara-id,chara-name")) 
			{
				sc.close();
				return null;
			}
			while (sc.hasNextLine())
			{
				rows.add(sc.nextLine());
				rowCnt++;
			}
			sc.close();
			
			String[] rowsAsArray = new String[rowCnt];
			rows.toArray(rowsAsArray);
			charaIdsFromCsv = new String[rowCnt];
			charaNamesFromCsv = new String[rowCnt];
			
			for (int i=0; i<rowCnt; i++) 
			{
				charaIdsFromCsv[i] = rowsAsArray[i].split(",")[0];
				charaNamesFromCsv[i] = rowsAsArray[i].split(",")[1];
			}
		}
		
		for (int i=0; i<charaIds.length; i++)
		{
			int searchIndex = Arrays.binarySearch(charaIdsFromCsv, charaIds[i]);
			if (searchIndex>=0) charaNames[i] = charaNamesFromCsv[searchIndex];
			else charaNames[i] = charaIds[i];
		}
		return charaNames;
	}
	public static void deleteSlot(RandomAccessFile bin, int deleteIdx) throws IOException
	{
		bin.seek(0);
		int currNumSlots = LittleEndian.getInt(bin.readInt());
		currNumSlots--;
		bin.seek(0);
		bin.writeInt(LittleEndian.getInt(currNumSlots));
		
		int binSize = (int)bin.length(), pos;
		for (int i=0; i<deleteIdx+2; i++)
		{
			pos = 4+(i*173);
			bin.seek(pos);
			if (i==deleteIdx+1) //deleteIdx+1 is the panel next to the deleted character panel
			{
				byte[] restOfFile = new byte[binSize-pos];
				bin.read(restOfFile);
				bin.seek(pos-173); //go back to the deleted character panel
				bin.write(restOfFile);
				bin.setLength(binSize-173);
			}
		}
		System.out.println("Slot "+deleteIdx+" has been deleted!");
		bin.close();
	}
	public static void insertSlot(RandomAccessFile bin, String charaId, int insertIdx) throws IOException
	{
		byte[] newSlot = getCharaPanelContents(charaId);
		int binSize = (int)bin.length();
		
		bin.seek(0);
		int currNumSlots = LittleEndian.getInt(bin.readInt());
		currNumSlots++;
		bin.seek(0);
		bin.writeInt(LittleEndian.getInt(currNumSlots));
		
		if (insertIdx==37 || insertIdx>numSlots)
		{
			int pos = 4+(numSlots*173);
			byte[] restOfFile = new byte[binSize-pos];
			bin.seek(pos);
			bin.read(restOfFile);
			bin.seek(pos);
			bin.write(newSlot);
			bin.write(restOfFile);
		}
		else
		{
			int pos;
			for (int i=0; i<insertIdx+1; i++)
			{
				pos = 4+(i*173);
				bin.seek(pos);
				if (i==insertIdx)
				{
					byte[] restOfFile = new byte[binSize-pos];
					bin.read(restOfFile);
					bin.seek(pos);
					bin.write(newSlot);
					bin.write(restOfFile);
				}
			}
		}
		System.out.println("New slot has been inserted at position "+insertIdx+"!");
		bin.close();
	}
	public static void overwriteSlot(RandomAccessFile bin, String charaId, int overwriteIdx) throws IOException
	{
		byte[] newSlot = getCharaPanelContents(charaId);
		int pos;
		for (int i=0; i<overwriteIdx+1; i++)
		{
			pos = 4+(i*173);
			bin.seek(pos);
			if (i==overwriteIdx) bin.write(newSlot);
		}
		System.out.println("Slot "+overwriteIdx+" has been overwritten!");
		bin.close();
	}
	public static void printRoster(String[] charaNames, int[] rowPanels)
	{
		int charaCnt=0, rowIdx=0;
		for (int i=0; i<charaNames.length; i++)
		{
			System.out.print("["+i+"] "+charaNames[i]+", ");
			charaCnt++;
			if (charaCnt==rowPanels[rowIdx])
			{
				System.out.println();
				rowIdx++;
				charaCnt=0;
			}
		}
		System.out.println();
	}
}