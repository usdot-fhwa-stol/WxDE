js.lang.System.createNamespace("js.util.Collections");


js.util.Collections.SIZE_THRESHOLD = 17;


js.util.Collections.binarySearch = function(oList, oKey, oCompare)
{
	var nLow = 0;

	if (oList != null)
	{
		var nHigh = oList.length - 1;

		while (nLow <= nHigh)
		{
			var nMid = (nLow + nHigh) >>> 1;
			var nCompare = oCompare.compare(oList[nMid], oKey);

			if (nCompare < 0)
				nLow = nMid + 1;
			else if (nCompare > 0)
				nHigh = nMid - 1;
			else
				return nMid; // key found
		}
	}

	return -(nLow + 1); // key not found
}


js.util.Collections.swap = function(oList, nLo, nHi)
{
	var oT = oList[nLo];
	oList[nLo] = oList[nHi];
	oList[nHi] = oT;
}


js.util.Collections.usort = function(oList, oCompare)
{
	js.util.Collections.usortRange(oList, oCompare, 0, oList.length);
}


js.util.Collections.usortRange = function(oList, oCompare, nBegin, nEnd)
{
	if (nBegin < nEnd)
	{
		var nSize = 0;
		for (var nValue = nEnd - nBegin; nValue != 1; nValue >>= 1)
			++nSize;

		js.util.Collections.introsortLoop(oList, oCompare, nBegin, nEnd, 2 * nSize);
		js.util.Collections.insertionsort(oList, oCompare, nBegin, nEnd);
	}
}


/*
 * Quicksort algorithm modified for Collections
 */
js.util.Collections.introsortLoop = function(oList, oCompare, nLo, nHi, nDepthLimit)
{
	while (nHi - nLo > js.util.Collections.SIZE_THRESHOLD)
	{
		if (nDepthLimit == 0)
		{
			js.util.Collections.heapsort(oList, oCompare, nLo, nHi);
			return;
		}
		--nDepthLimit;

		var p = js.util.Collections.partition(oList, oCompare, nLo, nHi,
			js.util.Collections.medianof3(oList, oCompare, nLo, nLo + (((nHi - nLo) / 2) | 0) + 1, nHi - 1));
		js.util.Collections.introsortLoop(oList, oCompare, p, nHi, nDepthLimit);
		nHi = p;
	}
}


js.util.Collections.partition = function(oList, oCompare, nLo, nHi, oT)
{
	for(;;)
	{
		while (oCompare.compare(oList[nLo], oT) < 0)
			nLo++;

		--nHi;
		while (oCompare.compare(oT, oList[nHi]) < 0)
			--nHi;

		if(nLo >= nHi)
			return nLo;

		js.util.Collections.swap(oList, nLo, nHi);
		nLo++;
	}
}


js.util.Collections.medianof3 = function(oList, oCompare, nLo, nMid, nHi)
{
	if (oCompare.compare(oList[nMid], oList[nLo]) < 0)
	{
		if (oCompare.compare(oList[nHi], oList[nMid]) < 0)
			return oList[nMid];
		else
		{
			if (oCompare.compare(oList[nHi], oList[nLo]) < 0)
				return oList[nHi];
			else
				return oList[nLo];
		}
	}
	else
	{
		if (oCompare.compare(oList[nHi], oList[nMid]) < 0)
		{
			if (oCompare.compare(oList[nHi], oList[nLo]) < 0)
				return oList[nLo];
			else
				return oList[nHi];
		}
		else
			return oList[nMid];
	}
}


/*
 * Heapsort algorithm
 */
js.util.Collections.heapsort = function(oList, oCompare, nLo, nHi)
{
	var n = nHi - nLo;
	var i = n / 2;
	for (; i >= 1; i--)
		js.util.Collections.downheap(oList, oCompare, i, n, nLo);

	for (i = n; i > 1; i--)
	{
		js.util.Collections.swap(oList, nLo, nLo + i - 1);
		js.util.Collections.downheap(oList, oCompare, 1, i - 1, nLo);
	}
}


js.util.Collections.downheap = function(oList, oCompare, i, n, nLo)
{
	var oT = oList[nLo + i - 1];
	while (i <= n / 2)
	{
		var child = 2 * i;
		if (child < n && oCompare.compare(oList[nLo + child - 1],
			oList[nLo + child]) < 0)
			child++;

		if (oCompare.compare(oT, oList[nLo + child - 1]) >= 0)
			break;

		oList[nLo + i - 1] = oList[nLo + child - 1];
		i = child;
	}
	oList[nLo + i - 1] = oT;
}


/*
 * Insertion sort algorithm
 */
js.util.Collections.insertionsort = function(oList, oCompare, nLo, nHi)
{
	for (var i = nLo; i < nHi; i++)
	{
		var j = i;
		var oT = oList[i];
		while(j != nLo && oCompare.compare(oT, oList[j - 1]) < 0)
		{
			oList[j] = oList[j - 1];
			j--;
		}
		oList[j] = oT;
	}
}
