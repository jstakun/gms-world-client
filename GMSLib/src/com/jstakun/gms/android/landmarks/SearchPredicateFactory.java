package com.jstakun.gms.android.landmarks;

import org.apache.commons.lang.StringUtils;

import android.text.Html;

import com.google.common.base.Predicate;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.deals.CategoriesManager;
import com.jstakun.gms.android.deals.Category;

public class SearchPredicateFactory {

	private static final SearchPredicateFactory instance = new SearchPredicateFactory(); 
	private static final int REVELANCE_LIMIT = 60;
	private static final int MIN_TOKEN_LENGTH = 2;
    
	private SearchPredicateFactory() {
	}
	
	protected static SearchPredicateFactory getInstance() {
		return instance;
	}
	
	protected Predicate<ExtendedLandmark> getSearchPredicate(int searchType, String[] searchTermTokens, String searchTerm) {
    	if (searchType == -1) {
    		searchType = ConfigurationManager.getInstance().getInt(ConfigurationManager.SEARCH_TYPE);
    	}
    	if (searchType == ConfigurationManager.WORDS_SEARCH) {
    		return new PhraseWordsSearchPredicate(searchTermTokens, false, searchTerm); //phrase words
        } else if (searchType == ConfigurationManager.FUZZY_SEARCH) {
            return new FuzzySearchPredicate(searchTermTokens, false, searchTerm); //fuzzy
        } else {
            //default exact phrase ConfigurationManager.PHRASE_SEARCH
            return new ExactPhraseSearchPredicate(searchTermTokens, searchTerm);
        }
    }
	
	private class ExactPhraseSearchPredicate implements Predicate<ExtendedLandmark> {

    	private String searchTerm, layerId;
    	private String[] query_tokens;
        
        public ExactPhraseSearchPredicate(String[] query_tokens, String searchTerm) {
            this.searchTerm = searchTerm;
            this.query_tokens = query_tokens;
            if (searchTerm != null) {
            	this.layerId = searchTerm + "_" + ConfigurationManager.PHRASE_SEARCH;
            } else {
            	this.layerId = query_tokens[0] + "_" + ConfigurationManager.PHRASE_SEARCH;
            }
        }

        public boolean apply(ExtendedLandmark landmark) {
        	Integer currentRevelance = landmark.gerLayerRevelance(layerId);
        	if (currentRevelance != null) {
        		if (currentRevelance > REVELANCE_LIMIT) {
        			return true;
        		} else {
        			return false;
        		}
        	} else if (searchTerm != null && StringUtils.equalsIgnoreCase(searchTerm, landmark.getSearchTerm())) {
                landmark.setLayerRevelance(layerId, 99);
                return true;
            } else if (searchTerm != null && (StringUtils.containsIgnoreCase(landmark.getName(), searchTerm)
                    || StringUtils.containsIgnoreCase(landmark.getDescription(), searchTerm)
                    || StringUtils.containsIgnoreCase(landmark.getLayer(), searchTerm))) {
                landmark.setLayerRevelance(layerId, 100);
                return true;
            } else {
            	
            	for (String query_token : query_tokens) {
                    if (StringUtils.containsIgnoreCase(landmark.getName(), query_token)
                    		|| StringUtils.containsIgnoreCase(landmark.getDescription(), query_token)
                    		|| StringUtils.containsIgnoreCase(landmark.getLayer(), query_token)) {
                    	landmark.setLayerRevelance(layerId, 100);
                    	return true;
                    }             
            	}
            	
                landmark.setLayerRevelance(layerId, 0);
                return false;
            }
        }
    }
	
	private class PhraseWordsSearchPredicate implements Predicate<ExtendedLandmark> {

        private String[] query_tokens;
        private CategoriesManager cm;
        private boolean searchCategories;
        private String searchTerm, layerId;
         
        public PhraseWordsSearchPredicate(String[] query_tokens, boolean searchCategories, String searchTerm) {
            this.query_tokens = query_tokens;
            this.searchCategories = searchCategories;
            this.searchTerm = searchTerm;
            this.cm = (CategoriesManager) ConfigurationManager.getInstance().getObject(ConfigurationManager.DEAL_CATEGORIES, CategoriesManager.class);
            this.layerId = query_tokens[0] + "_" + ConfigurationManager.WORDS_SEARCH;
        }

        //check if landmarks contains all tokens
        public boolean apply(ExtendedLandmark landmark) {
            int revelance = 0;

            Integer currentRevelance = landmark.gerLayerRevelance(layerId);
        	if (currentRevelance != null) {
        		if (currentRevelance > REVELANCE_LIMIT) {
        			return true;
        		} else {
        			return false;
        		}
        	} else if (searchTerm != null && StringUtils.equalsIgnoreCase(searchTerm, landmark.getSearchTerm())) {
                revelance = 99;
            } else {
                String name = Html.fromHtml(landmark.getName()).toString();
                String[] name_tokens = getTokens(name);
                String desc = Html.fromHtml(landmark.getDescription()).toString();
                String[] desc_tokens = getTokens(desc);

                for (String query_token : query_tokens) {
                    boolean tokenMatching = false;
                    for (String name_token : name_tokens) {
                        if (StringUtils.equalsIgnoreCase(name_token, query_token)) {
                            int percent = 100;
                            if (revelance == 0 || revelance < percent) {
                                revelance = percent;
                            }
                            tokenMatching = true;
                            break;
                        } else {
                            if (searchCategories) {
                                int catId = landmark.getCategoryId();
                                int subCatId = landmark.getSubCategoryId();
                                Category cat = cm.getCategory(catId);
                                if (catId != -1 && cat != null && StringUtils.containsIgnoreCase(cat.getCategory(), query_token)) {
                                    revelance = REVELANCE_LIMIT;
                                }
                                Category subCat = cm.getSubCategory(catId, subCatId);
                                if (catId != -1 && subCatId != -1 && subCat != null && StringUtils.containsIgnoreCase(subCat.getSubcategory(), query_token)) {
                                    revelance = 80;
                                }
                            }
                            int dist = StringUtils.getLevenshteinDistance(name_token, query_token);
                            int percent = (int) ((1.0 - (double) dist / (double) Math.max(name_token.length(), query_token.length())) * 100.0);
                            if (percent > REVELANCE_LIMIT) {
                                //System.out.println("Found similar tokens: " + name_tokens[i] + "-" + query_tokens[j] + ", with score " + percent);
                                if (revelance == 0 || revelance < percent) {
                                    revelance = percent;
                                }
                                tokenMatching = true;
                                break;
                            }
                        }
                    }

                    if (!tokenMatching) {
                        for (String desc_token : desc_tokens) {
                            if (StringUtils.equalsIgnoreCase(desc_token, query_token)) {
                                int percent = 100;
                                if (revelance == 0 || revelance < percent) {
                                    revelance = percent;
                                }
                                //tokenMatching = true;
                                break;
                            } else {
                                int dist = StringUtils.getLevenshteinDistance(desc_token, query_token);
                                int percent = (int) ((1.0 - (double) dist / (double) Math.max(desc_token.length(), query_token.length())) * 100.0);
                                if (percent > REVELANCE_LIMIT) {
                                    //System.out.println("Found similar tokens: " + desc_tokens[i] + "-" + query_tokens[j] + ", with score " + percent);
                                    if (revelance == 0 || revelance < percent) {
                                        revelance = percent;
                                    }
                                    //tokenMatching = true;
                                    break;
                                }
                            }
                        }
                    }

                    if (revelance == 0) {
                        landmark.setLayerRevelance(layerId, 0);
                        return false;
                    }
                }
            }

            landmark.setLayerRevelance(layerId, revelance);
            return true;
        }
        
        private String[] getTokens(String input) {
        	return StringUtils.split(input, " .,:\n");
        }
    }
	
	private class FuzzySearchPredicate implements Predicate<ExtendedLandmark> {

        private String[] query_tokens;
        private CategoriesManager cm;
        private boolean searchCategories;
        private String searchTerm, layerId;
        
        public FuzzySearchPredicate(String[] query_tokens, boolean searchCategories, String searchTerm) {
            this.query_tokens = query_tokens;
            this.searchCategories = searchCategories;
            this.searchTerm = searchTerm;
            this.cm = (CategoriesManager) ConfigurationManager.getInstance().getObject(ConfigurationManager.DEAL_CATEGORIES, CategoriesManager.class);
            this.layerId = query_tokens[0] + "_" + ConfigurationManager.FUZZY_SEARCH;
        }

        //check if landmarks contains any token
        public boolean apply(ExtendedLandmark landmark) {
        	Integer currentRevelance = landmark.gerLayerRevelance(layerId);
        	if (currentRevelance != null) {
        		if (currentRevelance > REVELANCE_LIMIT) {
        			return true;
        		} else {
        			return false;
        		}
        	}
        	if (searchTerm != null && StringUtils.equalsIgnoreCase(searchTerm, landmark.getSearchTerm())) {
                landmark.setLayerRevelance(layerId, 99);
                return true;
            } else {
            	String name = Html.fromHtml(landmark.getName()).toString();
            	String[] name_tokens = getTokens(name);
                for (String name_token : name_tokens) {
                	if (name_token.length() > MIN_TOKEN_LENGTH) {
                		for (String query_token : query_tokens) {	
                			if (StringUtils.equalsIgnoreCase(name_token, query_token)) {
                				landmark.setLayerRevelance(layerId, 100);
                				return true;
                        	} else {
                            	if (searchCategories) {
                                	int catId = landmark.getCategoryId();
                                	int subCatId = landmark.getSubCategoryId();
                                	Category cat = cm.getCategory(catId);
                                	if (catId != -1 && cat != null && StringUtils.containsIgnoreCase(cat.getCategory(), query_token)) {
                                		landmark.setLayerRevelance(layerId, REVELANCE_LIMIT);
                                	}
                                	Category subCat = cm.getSubCategory(catId, subCatId);
                                	if (catId != -1 && subCatId != -1 && subCat != null && StringUtils.containsIgnoreCase(subCat.getSubcategory(), query_token)) {
                                		landmark.setLayerRevelance(layerId, 80);
                                	}
                            	}

                            	int dist = StringUtils.getLevenshteinDistance(name_token, query_token);
                            	int percent = (int) ((1.0 - (double) dist / (double) Math.max(name_token.length(), query_token.length())) * 100.0);
                            	if (percent > REVELANCE_LIMIT) {
                            		landmark.setLayerRevelance(layerId, percent);
                                	return true;
                            	}
                        	}
                    	}
                    }
                }

                String desc = Html.fromHtml(landmark.getDescription()).toString();
                if (desc != null) {
                    String[] desc_tokens = getTokens(desc);
                    for (String desc_token : desc_tokens) {               	
                    	if (desc_token.length() > MIN_TOKEN_LENGTH) {
                    		for (String query_token : query_tokens) {
                    			if (StringUtils.equalsIgnoreCase(desc_token, query_token)) {
                    				landmark.setLayerRevelance(layerId, 100);
                    				return true;
                    			} else {
                    				int dist = StringUtils.getLevenshteinDistance(desc_token, query_token);
                    				int percent = (int) ((1.0 - (double) dist / (double) Math.max(desc_token.length(), query_token.length())) * 100.0);
                    				if (percent > REVELANCE_LIMIT) {
                    					landmark.setLayerRevelance(layerId, percent);
                    					return true;
                    				}
                    			}
                    		}
                    	}
                    }
                }
            }

            landmark.setLayerRevelance(layerId, 0);
            return false;
        }
        
        /*private String[] getHtmlTokens(String input) {
        	String[] lines = StringUtils.splitByWholeSeparator(input, "<br/>");
        	String[] res = new String[]{};
        	
        	for (String l : lines) {
        		String[] tokens = StringUtils.split(l, " .,:\n");
        		res = ObjectArrays.concat(res, tokens, String.class);
        	}
        	
        	return res;
        }*/
        
        private String[] getTokens(String input) {
        	return StringUtils.split(input, " .,:\n");
        }
    }
}
