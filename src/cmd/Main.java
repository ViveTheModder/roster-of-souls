package cmd;
//Roster of Souls v1.1 by ViveTheModder
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

public class Main 
{
	private static int rowCnt=0, numSlots;
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
	private static int getSlotIdFromCharaName(String[] charaNames, String name)
	{
		int slotId=-1;
		//linear search (oh lord) if the character name is already in the roster
		for (int i=0; i<charaNames.length; i++)
			if (name.equals(charaNames[i])) slotId=i;
		return slotId;
	}
	private static int[] getNumPanelsPerRow(RandomAccessFile bin) throws IOException
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
	private static String getCharaIdFromName(String charaName)
	{
		String charaId=null;
		//oh wow, another linear search (please lord) is needed
		for (int i=0; i<charaNamesFromCsv.length; i++)
			if (charaName.equals(charaNamesFromCsv[i])) charaId = charaIdsFromCsv[i];
		return charaId;
	}
	private static String[] getCharaIds(RandomAccessFile bin) throws IOException
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
	private static String[] getCharaNames(String[] charaIds) throws IOException
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
	public static void main(String[] args) throws IOException, URISyntaxException
	{	
		int option=-1;
		File[] binFiles = new File[2];
		RandomAccessFile[] bins = new RandomAccessFile[2];
		String[] fileNames = {"CharaSelect","Character_Select"};
		String[] options = {"Exit","Print Roster","Insert New Slot","Overwrite Slot","Remove Slot"};
		Scanner sc = new Scanner(System.in);
		
		for (int i=0; i<2; i++)
		{
			while (binFiles[i]==null)
			{
				System.out.println("Enter a valid path to the "+fileNames[i]+".bin file:");
				String path = sc.nextLine();
				File tmp = new File(path);
				if (tmp.isFile()) 
				{
					if (tmp.getName().toLowerCase().equals(fileNames[i].toLowerCase()+".bin")) 
					{
						binFiles[i]=tmp;
						bins[i]=new RandomAccessFile(binFiles[i],"rw");
					}
					else System.out.println("Path does NOT point to the exact "+fileNames[i]+".bin file. Try again!\n");
				}
				else if (tmp.isDirectory()) System.out.println("Path does NOT point to a file, but to a directory. Try again!\n");
			}
		}
		while (true)
		{
			bins[0]=new RandomAccessFile(binFiles[0],"rw"); //this reset is required to prevent the stream from closing
			System.out.println("\nEnter a valid option number out of the following options:");
			for (int i=0; i<options.length; i++) System.out.println(i+". "+options[i]);
			String input = sc.nextLine();
			if (input.matches("[0-"+(options.length-1)+"]+")) option = Integer.parseInt(input);
			else System.out.println("Invalid option number or format. Try again!\n");
			
			if (option==0 || numSlots>38) 
			{
				sc.close();
				break;
			}
			
			int[] rowPanels = getNumPanelsPerRow(bins[1]);
			String charaName=null;
			String[] charaIds = getCharaIds(bins[0]);
			String[] charaNames = getCharaNames(charaIds);
			switch (option)
			{
				case 1: 
					printRoster(charaNames, rowPanels);
					break;
				case 2:
					int insertIdx=-1;
					charaName=null;
					while (insertIdx==-1)
					{
						System.out.println("Specify an index (starting from zero) to insert a slot.\n"
						+ "Otherwise, specify N for no index (slot is inserted at the end).\n"
						+ "DISCLAIMER: Inserting slots for non-playable characters will lead\n"
						+ "to a softlock, unless their character selects animations are edited.");
						String idxOption = sc.nextLine();
						if (idxOption.matches("\\d+"))
						{
							int tmp = Integer.parseInt(idxOption);
							if (tmp<=37) insertIdx=tmp;
							else System.out.println("Index out of range (exceeds 37). Try again!\n");
						}
						else if (idxOption.toLowerCase().equals("n")) insertIdx=37;
						else System.out.println("Invalid option (not an index or the letter N). Try again!\n");
					}
					while (charaName==null)
					{
						System.out.println("Enter a valid character name:");
						String name = sc.nextLine();
						int slotId = getSlotIdFromCharaName(charaNames,name);
						if (slotId>=0) 
						{
							insertSlot(bins[0],charaIds[slotId],insertIdx);
							charaName=name;
						}
						else 
						{
							String newCharaId = getCharaIdFromName(name);
							if (newCharaId!=null)
							{
								insertSlot(bins[0],newCharaId,insertIdx);
								charaName=name;
							}
							else System.out.println("Invalid character name. Try again!\n");
						}
					}
					break;
				case 3:
					int overwriteIdx=-1;
					charaName=null;
					while (overwriteIdx==-1)
					{
						System.out.println("Specify an index (starting from zero) to represent the overwritten slot.\n"
						+ "DISCLAIMER: Overwriting slots to use non-playable characters will lead\n"
						+ "to a softlock, unless their character select animations are edited.");
						String idxOption = sc.nextLine();
						if (idxOption.matches("\\d+"))
						{
							int tmp = Integer.parseInt(idxOption);
							if (tmp<numSlots) overwriteIdx=tmp;
							else System.out.println("Index out of range (is/exceeds the number of slots). Try again!\n");
						}
						else System.out.println("Invalid option (not an index). Try again!\n");
					}
					while (charaName==null)
					{
						System.out.println("Enter a valid character name:");
						String name = sc.nextLine();
						int slotId = getSlotIdFromCharaName(charaNames,name);
						if (slotId>=0) 
						{
							overwriteSlot(bins[0],charaIds[slotId],overwriteIdx);
							charaName=name;
						}
						else 
						{
							String newCharaId = getCharaIdFromName(name);
							if (newCharaId!=null)
							{
								overwriteSlot(bins[0],newCharaId,overwriteIdx);
								charaName=name;
							}
							else System.out.println("Invalid character name. Try again!\n");
						}
					}
					break;
				case 4:
					int deleteIdx=-1;
					while (deleteIdx==-1)
					{
						System.out.println("Specify an index (starting from zero) to represent the deleted slot:");
						String idxOption = sc.nextLine();
						if (idxOption.matches("\\d+"))
						{
							int tmp = Integer.parseInt(idxOption);
							if (tmp<numSlots) deleteIdx=tmp;
							else System.out.println("Index out of range (is/exceeds the number of slots). Try again!\n");
						}
						else System.out.println("Invalid option (not an index). Try again!\n");
					}
					deleteSlot(bins[0],deleteIdx);
					break;
			}
		}		
	}
}