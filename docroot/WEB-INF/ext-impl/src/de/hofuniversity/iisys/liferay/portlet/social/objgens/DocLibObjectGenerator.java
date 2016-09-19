package de.hofuniversity.iisys.liferay.portlet.social.objgens;

import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portlet.documentlibrary.model.DLFolder;
import com.liferay.portlet.documentlibrary.service.DLAppLocalServiceUtil;
import com.liferay.portlet.documentlibrary.service.DLFolderServiceUtil;

import de.hofuniversity.iisys.liferay.portlet.social.model.ActivityObjectContainer;

public class DocLibObjectGenerator implements IObjectGenerator
{
	private final String fServiceUrl;

	public DocLibObjectGenerator(String url)
	{
		fServiceUrl = url;
	}
	
	@Override
	public ActivityObjectContainer generate(String className, long primaryKey)
	{
		ActivityObjectContainer container = null;
		
		if("com.liferay.portlet.documentlibrary.model.DLFileEntry".equals(className))
		{
			container = docLibEntry(className, primaryKey);
		}
		else if("com.liferay.portlet.documentlibrary.model.DLFolder".equals(className))
		{
			container = docLibFolder(className, primaryKey);
		}
		
		return container;
	}

	private ActivityObjectContainer docLibEntry(String className, long primaryKey)
	{
		ActivityObjectContainer container = null;
		
		FileEntry fileEntry = null;
		
		try
		{
			fileEntry = DLAppLocalServiceUtil.getFileEntry(primaryKey);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
		String fileLink = fServiceUrl + "/document_library/find_file_entry?fileEntryId=" +
				primaryKey;
		
		container = new ActivityObjectContainer(
				"liferay-document-library-file", Long.toString(primaryKey),
				fileEntry.getTitle(), fileLink);
		if(fileEntry.getDescription() != null)
		{
			container.setContent(fileEntry.getDescription());
		}
		
		return container;
	}

	private ActivityObjectContainer docLibFolder(String className, long primaryKey)
	{
		ActivityObjectContainer container = null;
		
		DLFolder folder = null;
		String name = null;
		
		try
		{
			folder = DLFolderServiceUtil.getFolder(primaryKey);
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
				primaryKey;
		
		container  = new ActivityObjectContainer("liferay-document-library-folder",
			Long.toString(primaryKey), name, folderLink);
		
		return container;
	}
}
