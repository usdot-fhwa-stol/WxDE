// bootstrap the System namespace
js = new Object();
js.lang = new Object();
js.lang.System = new Object();

js.lang.System.createNamespace = function(sNamespace)
{
	var sParts = sNamespace.split(".");
	var oRoot = window;

	for(var nIndex = 0; nIndex < sParts.length; nIndex++)
	{
		if(oRoot[sParts[nIndex]] == undefined)
			oRoot[sParts[nIndex]] = new Object();

		oRoot = oRoot[sParts[nIndex]];
	}
};
