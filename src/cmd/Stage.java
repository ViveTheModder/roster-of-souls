package cmd;
//Rebirth of Souls - Stage Class
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Scanner;

//TODO: import other methods from Chara class

public class Stage 
{
	public static int numSlots;
	private static int rowCnt=0, startOfStageInfo;
	private static String[] stageIdsFromCsv, stageNamesFromCsv;

	private static byte[] getStagePanelContents(String stageId) throws IOException
	{
		byte[] stageIdBytes = new byte[20];
		byte[] stagePanelContents = new byte[68], intersection = new byte[68], placeholder = new byte[68];
		int pos=startOfStageInfo, sectionSize=(68*numSlots)+pos;
		File charaSelectVanilla = new File("./bak/CharaSelectVanilla.bin");
		RandomAccessFile raf = new RandomAccessFile(charaSelectVanilla,"r");
		while (pos<sectionSize)
		{
			raf.read(placeholder);
			pos+=68;
			System.arraycopy(placeholder, 32, stageIdBytes, 0, 20);
			String currStageId = new String(stageIdBytes);
			char[] currStageIdAsCharArray = currStageId.toCharArray();
			if (currStageIdAsCharArray.length==20) currStageId = currStageId.substring(0,19);
				
			if (currStageId.equals(stageId)) System.arraycopy(placeholder, 0, stagePanelContents, 0, 68);
			else if (currStageId.equals("battle_stage_name_1")) System.arraycopy(placeholder, 0, intersection, 0, 68);
			raf.seek(pos);
		}
		//if stage ID is not present, then make use of placeholder data from [Karakura Town] Intersection - Day
		if (stagePanelContents[0]==0)
		{
			System.arraycopy(stageId.getBytes(), 0, intersection, 32, stageId.getBytes().length);
			System.arraycopy(intersection, 0, stagePanelContents, 0, 68);
		}
		raf.close();
		return stagePanelContents;
	}
	public static int getSlotIdFromStageName(String[] stageNames, String name)
	{
		int slotId=-1;
		//linear search (oh lord) if the character name is already in the roster
		for (int i=0; i<stageNames.length; i++)
			if (name.equals(stageNames[i])) slotId=i;
		return slotId;
	}
	public static String getStageIdFromName(String charaName)
	{
		String stageId=null;
		//oh wow, another linear search (please lord) is needed
		for (int i=0; i<stageNamesFromCsv.length; i++)
			if (charaName.equals(stageNamesFromCsv[i])) stageId = stageIdsFromCsv[i];
		return stageId;
	}
	public static String[] getStageIds(RandomAccessFile bin) throws IOException
	{
		bin.seek(0);
		int numSlotsChara = LittleEndian.getInt(bin.readInt());
		bin.seek((numSlotsChara*173)+4);
		startOfStageInfo = (int)bin.getFilePointer()+4;
		
		byte[] stageIdBytes = new byte[20];
		numSlots=LittleEndian.getInt(bin.readInt());
		int stageCnt=0, pos=(int)bin.getFilePointer(), sectionSize=(68*numSlots)+pos;
		String[] stageIds = new String[numSlots];
		while (pos<sectionSize && stageCnt<numSlots)
		{
			pos+=32; //skip BGM
			bin.seek(pos);
			
			bin.read(stageIdBytes);
			pos+=20;
			String stageId = new String(stageIdBytes);
			stageIds[stageCnt] = stageId;
			stageCnt++; 
			pos+=16; //rest of stage slot info
			bin.seek(pos);
		}
		return stageIds;
	}
	public static String[] getStageNames(String[] stageIds) throws IOException
	{
		String[] stageNames = new String[stageIds.length];
		if (rowCnt==0) //initialize static string arrays only once
		{
			File stageNamesCsv = new File("./csv/stage.csv");
			Scanner sc = new Scanner(stageNamesCsv,"ISO-8859-1");
			ArrayList<String> rows = new ArrayList<String>();
			String header = sc.nextLine();
			if (!header.equals("stage-id,stage-name")) 
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
			stageIdsFromCsv = new String[rowCnt];
			stageNamesFromCsv = new String[rowCnt];
			
			for (int i=0; i<rowCnt; i++) 
			{
				stageIdsFromCsv[i] = rowsAsArray[i].split(",")[0];
				stageNamesFromCsv[i] = rowsAsArray[i].split(",")[1];
			}
		}
		
		/* binary search CANNOT be used because for example, battle_stage_name_10 comes AFTER battle_stage_name_1
		   however, in the stage.csv file, battle_stage_name_2 is what comes after (as it logically should be)
		   the only solution would be to just add a zero before the last digit (01, 02, and so on), but that
		   would kinda mess things up for the CharaSelect.bin file, because they started counting from 1 rather than 01 */
		for (int i=0; i<stageIds.length; i++)
		{
			String stageIdCopy;
			for (int j=0; j<stageIdsFromCsv.length; j++)
			{
				//because 20 bytes are always scanned, the last byte must be removed if the stage ID is a single digit
				char[] stageIdAsCharArray = stageIds[i].toCharArray();
				if (stageIdAsCharArray[19]==0) stageIdCopy = stageIds[i].substring(0,19);
				else stageIdCopy = stageIds[i];
					
				if (stageIdCopy.equals(stageIdsFromCsv[j]))
				{
					stageNames[i] = stageNamesFromCsv[j];
					break;
				}
				else stageNames[i] = stageIdCopy;
			}
		}
		return stageNames;
	}
	public static void deleteSlot(RandomAccessFile bin, int deleteIdx) throws IOException
	{
		bin.seek(startOfStageInfo-4);
		int currNumSlots = LittleEndian.getInt(bin.readInt());
		currNumSlots--;
		bin.seek(startOfStageInfo-4);
		bin.writeInt(LittleEndian.getInt(currNumSlots));
		
		int binSize = (int)bin.length(), pos = startOfStageInfo;
		for (int i=0; i<deleteIdx+2; i++)
		{
			pos+=(i*68);
			bin.seek(pos);
			if (i==deleteIdx+1) //deleteIdx+1 is the panel next to the deleted character panel
			{
				byte[] restOfFile = new byte[binSize-pos];
				bin.read(restOfFile);
				bin.seek(pos-68); //go back to the deleted character panel
				bin.write(restOfFile);
				bin.setLength(binSize-68);
			}
		}
		System.out.println("Slot "+deleteIdx+" has been deleted!");
		bin.close();
	}
	public static void insertSlot(RandomAccessFile bin, String charaId, int insertIdx) throws IOException
	{
		byte[] newSlot = getStagePanelContents(charaId);
		int binSize = (int)bin.length();
		
		bin.seek(startOfStageInfo-4);
		int currNumSlots = LittleEndian.getInt(bin.readInt());
		currNumSlots++;
		bin.seek(startOfStageInfo-4);
		bin.writeInt(LittleEndian.getInt(currNumSlots));
		
		if (insertIdx==numSlots-1 || insertIdx>numSlots)
		{
			int pos = startOfStageInfo+(numSlots*68);
			byte[] restOfFile = new byte[binSize-pos];
			bin.seek(pos);
			bin.read(restOfFile);
			bin.seek(pos);
			bin.write(newSlot);
			bin.write(restOfFile);
		}
		else
		{
			int pos=startOfStageInfo;
			for (int i=0; i<insertIdx+1; i++)
			{
				pos+=(i*68);
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
		byte[] newSlot = getStagePanelContents(charaId);
		int pos = startOfStageInfo;
		for (int i=0; i<overwriteIdx+1; i++)
		{
			pos+=(i*68);
			bin.seek(pos);
			if (i==overwriteIdx) 
			{
				System.out.println(pos);
				bin.write(newSlot);
			}
		}
		System.out.println("Slot "+overwriteIdx+" has been overwritten!");
		bin.close();
	}
	public static void printRoster(String[] stageNames)
	{
		for (int i=0; i<stageNames.length; i++)
			System.out.println("("+i+") "+stageNames[i]);
	}
}