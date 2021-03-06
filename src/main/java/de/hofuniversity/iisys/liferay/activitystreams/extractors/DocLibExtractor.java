package de.hofuniversity.iisys.liferay.activitystreams.extractors;

import java.util.Locale;
import java.util.Map;

import com.liferay.document.library.kernel.model.DLFolder;
import com.liferay.document.library.kernel.service.DLAppLocalServiceUtil;
import com.liferay.document.library.kernel.service.DLFolderServiceUtil;
import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.service.GroupServiceUtil;
import com.liferay.social.kernel.model.SocialActivity;
import com.liferay.social.kernel.model.SocialActivityConstants;

import de.hofuniversity.iisys.liferay.activitystreams.model.ActivityObjectContainer;
import de.hofuniversity.iisys.liferay.activitystreams.model.SocialActivityContainer;

public class DocLibExtractor implements IActivityExtractor
{
	public static final String DL_FILE_ENTRY_CLASS =
			"com.liferay.document.library.kernel.model.DLFileEntry";
	public static final String DL_FOLDER_CLASS =
			"com.liferay.document.library.kernel.model.DLFolder";
	
	private final Locale fLocale;
	private final String fServiceUrl;
	
	private final String fFolderName;
	
	private final ActivityObjectContainer fGenerator;

	public DocLibExtractor(Map<String, String> properties, Locale locale, String url)
	{
		fLocale = locale;
		fServiceUrl = url;
		
		fFolderName = properties.get("folder_display_name");
		
		fGenerator = new ActivityObjectContainer("application", "liferay-document-library",
				"Liferay Document Library", null);
	}

	@Override
	public void extract(SocialActivity activity,
			SocialActivityContainer container)
	{
		String className = activity.getClassName();
		
		if(DL_FILE_ENTRY_CLASS.equals(className))
		{
			docLibEntry(activity, container);
		}
		else if(DL_FOLDER_CLASS.equals(className))
		{
			docLibFolder(activity, container);
		}
		
	}
	
	private void docLibEntry(SocialActivity activity, SocialActivityContainer container)
	{
		FileEntry fileEntry = null;
		
		try
		{
			fileEntry = DLAppLocalServiceUtil.getFileEntry(activity.getClassPK());
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
		//get possibly linked group object
		Group group = null;
		try
		{
			group = GroupServiceUtil.getGroup(activity.getGroupId());
		}
		catch(Exception e) {}
		
		final int type = activity.getType();
		String verb = null;
		String titleVar = null;
		String title = null;
		
		switch(type)
		{
			//TODO: replace once available
			//case DLActivityKeys.ADD_FILE_ENTRY:
		    case 1:
				titleVar = "activity-document-library-file-add-file";
				verb = "add";
				break;
				
			//case DLActivityKeys.UPDATE_FILE_ENTRY:
		    case 2:
				titleVar = "activity-document-library-file-update-file";
				verb = "update";
				break;
				
			case SocialActivityConstants.TYPE_MOVE_TO_TRASH:
				titleVar = "activity-document-library-file-move-to-trash";
				break;
				
			case SocialActivityConstants.TYPE_RESTORE_FROM_TRASH:
				titleVar = "activity-document-library-file-restore-from-trash";
				break;
		}
		
		//get different messages if a group is involved
		if(titleVar != null)
		{
			if(group != null)
			{
				title = LanguageUtil.get(fLocale, titleVar + "-in");
			}
			else
			{
				title = LanguageUtil.get(fLocale, titleVar);
			}
		}
		
		if(verb != null)
		{
			container.setVerb(verb);
		}
		
		if(title != null)
		{
			container.setTitle(title);
		}
		
		//add folder activity object
		String fileLink = fServiceUrl + "/document_library/find_file_entry?fileEntryId=" +
				activity.getClassPK();
		
		ActivityObjectContainer object = new ActivityObjectContainer(
				"liferay-document-library-file", Long.toString(activity.getClassPK()),
				fileEntry.getTitle(), fileLink);
		if(fileEntry.getDescription() != null)
		{
			object.setContent(fileEntry.getDescription());
		}
		
		container.setObject(object);
		
		
		//add file entry activity object
		DLFolder folder = null;
		String folderName = null;
		
		try
		{
			folder = DLFolderServiceUtil.getFolder(fileEntry.getFolderId());
			folderName = folder.getName();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}

		if(folderName == null)
		{
			folderName = fFolderName;
		}
		
		String folderLink = fServiceUrl + "/document_library/find_folder?groupId="
			+ fileEntry.getRepositoryId() + "&folderId=" + fileEntry.getFolderId();

		ActivityObjectContainer target = new ActivityObjectContainer(
				"liferay-document-library-folder", Long.toString(fileEntry.getFolderId()),
				folderName, folderLink);
		
		container.setTarget(target);
		
		//attach the blogs part as generator
		container.setGenerator(fGenerator);
	}
	
	private void docLibFolder(SocialActivity activity, SocialActivityContainer container)
	{
		//get possibly linked group object
		Group group = null;
		try
		{
			group = GroupServiceUtil.getGroup(activity.getGroupId());
		}
		catch(Exception e) {}
		
		final int type = activity.getType();
		String titleVar = null;
		String title = null;
		
		if(SocialActivityConstants.TYPE_MOVE_TO_TRASH == type)
		{
			titleVar = "activity-document-library-folder-move-to-trash";
		}
		else if(SocialActivityConstants.TYPE_RESTORE_FROM_TRASH == type)
		{
			titleVar = "activity-document-library-folder-restore-from-trash";
		}
		
		//get different messages if a group is involved
		if(titleVar != null)
		{
			if(group != null)
			{
				title = LanguageUtil.get(fLocale, titleVar + "-in");
			}
			else
			{
				title = LanguageUtil.get(fLocale, titleVar);
			}
		}
		
		if(title != null)
		{
			container.setTitle(title);
		}
		
		//add folder activity object
		DLFolder folder = null;
		String name = null;
		
		try
		{
			folder = DLFolderServiceUtil.getFolder(activity.getClassPK());
			name = folder.getName();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
		if(name == null)
		{
			name = "folder";
		}
		
		String folderLink = fServiceUrl + "/document_library/find_folder?folderId=" +
				activity.getClassPK();
		
		ActivityObjectContainer object = new ActivityObjectContainer(
				"liferay-document-library-folder", Long.toString(activity.getClassPK()),
				name, folderLink);
		
		container.setObject(object);
		
		//attach the blogs part as generator
		container.setGenerator(fGenerator);
	}

}
