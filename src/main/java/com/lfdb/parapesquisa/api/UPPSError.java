package com.lfdb.parapesquisa.api;

/**
 * Created by Igor on 7/29/13.
 */
public class UPPSError
{
    EResult mResult;
    String mDescription;

    public UPPSError()
    {

    }

    public UPPSError(EResult result, String description)
    {
        this.mResult = result;
        this.mDescription = description;
    }

    public String getDescription()
    {
        return mDescription;
    }

    public EResult getResult()
    {
        return mResult;
    }

    public void setDescription(String description)
    {
        this.mDescription = description;
    }

    public void setResult(EResult result)
    {
        this.mResult = result;
    }

    public boolean hasResult(EResult result)
    {
        return (this.mResult.ordinal() & result.ordinal()) == result.ordinal();
    }
}
