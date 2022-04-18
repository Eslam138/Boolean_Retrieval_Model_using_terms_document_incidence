//====================================================================================================
//The Free Edition of C# to Java Converter limits conversion output to 100 lines per file.

//To purchase the Premium Edition, visit our website:
//https://www.tangiblesoftwaresolutions.com/order/order-csharp-to-java.html
//====================================================================================================

package booleanretrievalmodel;

import java.util.*;
import java.nio.file.*;

/** 
 Implementation Simple Boolean Retrieval Model
 DPK 'SAMIR'
*/

public class BooleanRetrievalModel
{
	//stores distinct terms
	public static HashSet<String> distinctTerm = new HashSet<String>();
	//stores document id and its contents without splitting
	public static HashMap<Integer, String> documentContentList = new HashMap<Integer, String>();
	//stores document and its terms collection
	public static HashMap<String, ArrayList<String>> documentCollection = new HashMap<String, ArrayList<String>>();
	public static HashMap<String, ArrayList<Integer>> termDocumentIncidenceMatrix = new HashMap<String, ArrayList<Integer>>();
	//stop words collection
	public static ArrayList<String> stopWords = new ArrayList<String>(Arrays.asList("ON", "OF", "THE", "AN", "A"));
	//boolean operators list
	public static String[] booleanOperator = new String[] {"AND", "OR", "NOT"};
	public static void main(String[] args)
	{
		int count = 0;
		//read all the text document on the specified directory; change this directory based on your machine
		for (String file : Directory.EnumerateFiles("F:\\IR\\", "*.txt"))
		{
			String contents = Files.readString(file);
			String[] termsCollection = RemoveStopsWords(contents.toUpperCase().split("[ ]", -1));
			for (String term : termsCollection)
			{
				//prepeare distinct terms collection
				//remove stop words
				if (!stopWords.contains(term))
				{
					distinctTerm.add(term);
				}
			}

			//add document and their terms collection
			documentCollection.put(file, termsCollection.ToList());
			//add document and its content for displaying the search result
			documentContentList.put(count, contents);
			count++;
		}

		termDocumentIncidenceMatrix = GetTermDocumentIncidenceMatrix(distinctTerm, documentCollection);
		System.out.println("Enter your boolean query");
		do
		{
			String query = new Scanner(System.in).nextLine();
			ArrayList<Integer> lst = ProcessQuery(query);
			count = 0;
			if (lst != null)
			{
				for (int a : lst)
				{
					if (a == 1)
					{
						System.out.println(documentContentList.get(count));
					}
					count++;
				}
			}
			else
			{
				System.out.println("No search result found");
			}

		} while (1 == 1);
	}

	/** 
	 Removes the query terms that doesnot appears on document
	 
	 @param str
	*/
	private static void FilterQueryTerm(tangible.RefObject<String[]> str)
	{
		ArrayList<String> _queryTerm = new ArrayList<String>();


		for (String queryTerm : str.refArgValue)
		{
			if (queryTerm.toUpperCase().equals("BUT") || termDocumentIncidenceMatrix.containsKey(queryTerm.toUpperCase()) || booleanOperator.Contains(queryTerm))
			{
				_queryTerm.add(queryTerm);

			}
		}

		str.refArgValue = _queryTerm.toArray(new String[0]);
	}

	//prepares Term Document Incidence Matrix
	public static HashMap<String, ArrayList<Integer>> GetTermDocumentIncidenceMatrix(HashSet<String> distinctTerms, HashMap<String,ArrayList<String>> documentCollection)
	{
		HashMap<String, ArrayList<Integer>> termDocumentIncidenceMatrix = new HashMap<String, ArrayList<Integer>>();
		ArrayList<Integer> incidenceVector = new ArrayList<Integer>();
		for (String term : distinctTerms)
		{
			//incidence vector for each terms
			incidenceVector = new ArrayList<Integer>();
			for (Map.Entry<String,ArrayList<String>> p : documentCollection.entrySet())
			{

				if (p.getValue().contains(term))
				{
					//document contains the term
					incidenceVector.add(1);

				}
				else
				{
					//document do not contains the term
					incidenceVector.add(0);
				}
			}
			termDocumentIncidenceMatrix.put(term, incidenceVector);

		}
		return termDocumentIncidenceMatrix;
	}


	//removes all stop words
	public static String[] RemoveStopsWords(String[] str)
	{
		ArrayList<String> terms = new ArrayList<String>();
		for (String term : str)
		{
			if (!stopWords.contains(term))
			{
				terms.add(term);
			}
		}
		return terms.toArray(new String[0]);
	}

	//process the boolean query
	public static ArrayList<Integer> ProcessQuery(String query)
	{

		//query boolean operator
		String bitWiseOp = "";
		String[] queryTerm = RemoveStopsWords(query.toUpperCase().split("[ ]", -1));

		//remove query term that doesnot appears on document collection
		tangible.RefObject<String[]> tempRef_queryTerm = new tangible.RefObject<String[]>(queryTerm);
		FilterQueryTerm(tempRef_queryTerm);
	queryTerm = tempRef_queryTerm.refArgValue;
		ArrayList<Integer> previousTermIncidenceV = null;
		ArrayList<Integer> nextTermsIncidenceV = null;
		//holds the bitwise operation result
		ArrayList<Integer> resultSet = null;
		//suppose on query X AND Y, X is previousTerm term and Y is nextTerm
		boolean hasPreviousTerm = false;
		boolean hasNotOperation = false;
		for (String term : queryTerm)
		{
			//is a term
			if (!booleanOperator.Contains(term) && !term.equals("BUT"))
			{
				//query case: structure AND NOT analysis
				if (hasNotOperation)
				{

					if (hasPreviousTerm)
					{
						nextTermsIncidenceV = ProcessBooleanOperator("NOT", GetTermIncidenceVector(term), nextTermsIncidenceV);
					}
					//query case: eg.NOT analysis
					else
					{
						previousTermIncidenceV = ProcessBooleanOperator("NOT", GetTermIncidenceVector(term), nextTermsIncidenceV);
						resultSet = previousTermIncidenceV;
					}
					hasNotOperation = false;
				}
				else if (!hasPreviousTerm)
				{
					previousTermIncidenceV = GetTermIncidenceVector(term);
					resultSet = previousTermIncidenceV;
					hasPreviousTerm = true;
				}
				else
				{

					nextTermsIncidenceV = GetTermIncidenceVector(term);
				}
			}
			else if (term.equals("NOT"))
			{
				//indicates that the  term in the next iteration should be complemented.
				hasNotOperation = true;
			}
			else
			{
				//'BUT' also should be evaluated as AND eg. structure BUT NOT semantic should be evaluated as structure AND NOT semantic
				if (term.equals("BUT"))
				{
					bitWiseOp = "AND";
				}
				else
				{
				bitWiseOp = term;
				}
			}

			if (nextTermsIncidenceV != null && !hasNotOperation)
			{
				resultSet = ProcessBooleanOperator(bitWiseOp, previousTermIncidenceV, nextTermsIncidenceV);
				previousTermIncidenceV = resultSet;
				hasPreviousTerm = true;
				nextTermsIncidenceV = null;
			}
		}

		return resultSet;
	}

	public static ArrayList<Integer> ProcessBooleanOperator(String op, ArrayList<Integer> previousTermV, ArrayList<Integer> nextTermV)
	{
		ArrayList<Integer> resultSet = new ArrayList<Integer>();

//====================================================================================================
//End of the allowed output for the Free Edition of C# to Java Converter.

//To purchase the Premium Edition, visit our website:
//https://www.tangiblesoftwaresolutions.com/order/order-csharp-to-java.html
//====================================================================================================
