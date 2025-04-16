package cmd;
//Roster of Souls v1.2 by ViveTheModder
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URISyntaxException;
import java.util.Scanner;

//TODO: Add other options for chooseOptionsStage()
public class Main 
{	
	public static void chooseOptionsChara(File[] binFiles, RandomAccessFile[] bins, Scanner sc, String[] options) throws IOException
	{
		int option=0;
		while (true)
		{
			System.out.println("\nEnter a valid option number out of the following options:");
			for (int i=0; i<options.length; i++) System.out.println(i+". "+options[i]);
			String input = sc.nextLine();
			if (input.matches("[0-"+(options.length-1)+"]+")) option = Integer.parseInt(input);
			else System.out.println("Invalid option number or format. Try again!\n");
			
			if (option==0 || Chara.numSlots>38) 
			{
				System.exit(0);
				break;
			}
			
			bins[0]=new RandomAccessFile(binFiles[0],"rw"); //this reset is required to prevent the stream from closing
			int[] rowPanels = Chara.getNumPanelsPerRow(bins[1]);
			String charaName=null;
			String[] charaIds = Chara.getCharaIds(bins[0]);
			String[] charaNames = Chara.getCharaNames(charaIds);
			switch (option)
			{
				case 1: 
					Chara.printRoster(charaNames, rowPanels);
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
						int slotId = Chara.getSlotIdFromCharaName(charaNames,name);
						if (slotId>=0) 
						{
							Chara.insertSlot(bins[0],charaIds[slotId],insertIdx);
							charaName=name;
						}
						else 
						{
							String newCharaId = Chara.getCharaIdFromName(name);
							if (newCharaId!=null)
							{
								Chara.insertSlot(bins[0],newCharaId,insertIdx);
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
							if (tmp<Chara.numSlots) overwriteIdx=tmp;
							else System.out.println("Index out of range (is/exceeds the number of slots). Try again!\n");
						}
						else System.out.println("Invalid option (not an index). Try again!\n");
					}
					while (charaName==null)
					{
						System.out.println("Enter a valid character name:");
						String name = sc.nextLine();
						int slotId = Chara.getSlotIdFromCharaName(charaNames,name);
						if (slotId>=0) 
						{
							Chara.overwriteSlot(bins[0],charaIds[slotId],overwriteIdx);
							charaName=name;
						}
						else 
						{
							String newCharaId = Chara.getCharaIdFromName(name);
							if (newCharaId!=null)
							{
								Chara.overwriteSlot(bins[0],newCharaId,overwriteIdx);
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
							if (tmp<Chara.numSlots) deleteIdx=tmp;
							else System.out.println("Index out of range (is/exceeds the number of slots). Try again!\n");
						}
						else System.out.println("Invalid option (not an index). Try again!\n");
					}
					Chara.deleteSlot(bins[0],deleteIdx);
					break;
			}
		}
	}
	public static void chooseOptionsStage(File[] binFiles, RandomAccessFile[] bins, Scanner sc, String[] options) throws IOException
	{
		int option=0;
		while (true)
		{
			System.out.println("\nEnter a valid option number out of the following options:");
			for (int i=0; i<options.length; i++) System.out.println(i+". "+options[i]);
			String input = sc.nextLine();
			if (input.matches("[0-"+(options.length-1)+"]+")) option = Integer.parseInt(input);
			else System.out.println("Invalid option number or format. Try again!\n");
			
			bins[0]=new RandomAccessFile(binFiles[0],"rw"); //this reset is required to prevent the stream from closing
			String stageName=null;
			String[] stageIds = Stage.getStageIds(bins[0]);
			String[] stageNames = Stage.getStageNames(stageIds);
			switch (option)
			{
				case 0: 
					System.exit(2);
					break;
				case 1:
					Stage.printRoster(stageNames);
					break;
				case 2:
					int insertIdx=-1;
					stageName=null;
					while (insertIdx==-1)
					{
						System.out.println("Specify an index (starting from zero) to insert a slot.\n"
						+ "Otherwise, specify N for no index (slot is inserted at the end).");
						String idxOption = sc.nextLine();
						if (idxOption.matches("\\d+"))
						{
							int tmp = Integer.parseInt(idxOption);
							if (tmp<=Stage.numSlots-1) insertIdx=tmp;
							else System.out.println("Index out of range (exceeds"+(Stage.numSlots-1)+"). Try again!\n");
						}
						else if (idxOption.toLowerCase().equals("n")) insertIdx=(Stage.numSlots-1);
						else System.out.println("Invalid option (not an index or the letter N). Try again!\n");
					}
					while (stageName==null)
					{
						System.out.println("Enter a valid stage name:");
						String name = sc.nextLine();
						int slotId = Stage.getSlotIdFromStageName(stageNames,name);
						if (slotId>=0) 
						{
							Stage.insertSlot(bins[0],stageIds[slotId],insertIdx);
							stageName=name;
						}
						else 
						{
							String newStageId = Stage.getStageIdFromName(name);
							if (newStageId!=null)
							{
								Stage.insertSlot(bins[0],newStageId,insertIdx);
								stageName=name;
							}
							else System.out.println("Invalid stage name. Try again!\n");
						}
					}
					break;
				case 3:
					int overwriteIdx=-1;
					stageName=null;
					while (overwriteIdx==-1)
					{
						System.out.println("Specify an index (starting from zero) to represent the overwritten slot.");
						String idxOption = sc.nextLine();
						if (idxOption.matches("\\d+"))
						{
							int tmp = Integer.parseInt(idxOption);
							if (tmp<Stage.numSlots) overwriteIdx=tmp;
							else System.out.println("Index out of range (is/exceeds the number of slots). Try again!\n");
						}
						else System.out.println("Invalid option (not an index). Try again!\n");
					}
					while (stageName==null)
					{
						System.out.println("Enter a valid stage name:");
						String name = sc.nextLine();
						int slotId = Stage.getSlotIdFromStageName(stageNames,name);
						if (slotId>=0) 
						{
							Stage.overwriteSlot(bins[0],stageIds[slotId],overwriteIdx);
							stageName=name;
						}
						else 
						{
							String newStageId = Stage.getStageIdFromName(name);
							if (newStageId!=null)
							{
								Stage.overwriteSlot(bins[0], newStageId, overwriteIdx);
								stageName=name;
							}
							else System.out.println("Invalid stage name. Try again!\n");
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
							if (tmp<Stage.numSlots) deleteIdx=tmp;
							else System.out.println("Index out of range (is/exceeds the number of slots). Try again!\n");
						}
						else System.out.println("Invalid option (not an index). Try again!\n");
					}
					Stage.deleteSlot(bins[0],deleteIdx);
					break;
			}
		}
	}
	public static void main(String[] args) throws IOException, URISyntaxException
	{	
		int option=-1;
		File[] binFiles = new File[2];
		RandomAccessFile[] bins = new RandomAccessFile[2];
		String[] fileNames = {"CharaSelect","Character_Select"};
		String[] rosterOpts = {"Exit","Character Roster","Stage Roster"};
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
			System.out.println("\nEnter a valid option out of the following options:");
			for (int i=0; i<rosterOpts.length; i++) System.out.println(i+". "+rosterOpts[i]);
			String input = sc.nextLine();
			if (input.matches("[0-"+(options.length-1)+"]+")) option = Integer.parseInt(input);
			else System.out.println("Invalid option number or format. Try again!\n");
			
			switch (option)
			{
				case 0:
					sc.close();
					break;
				case 1:
					chooseOptionsChara(binFiles, bins, sc, options);
					break;
				case 2:
					chooseOptionsStage(binFiles, bins, sc, options);
					break;  			
			}
		}		
	}
}