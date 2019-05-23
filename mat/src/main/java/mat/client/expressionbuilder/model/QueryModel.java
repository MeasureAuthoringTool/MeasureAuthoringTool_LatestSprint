package mat.client.expressionbuilder.model;

import mat.client.expressionbuilder.constant.CQLType;
import mat.client.expressionbuilder.constant.ExpressionType;

public class QueryModel extends ExpressionBuilderModel {

	private ExpressionBuilderModel source;
	private String alias;
	private ExpressionBuilderModel filter;
	private QuerySortModel sort;
	private ExpressionBuilderModel relationship;
	private String relationshipType;	
	
	public QueryModel(ExpressionBuilderModel source, String alias, ExpressionBuilderModel filter, ExpressionBuilderModel parent) {
		super(parent);
		this.source = source;
		this.source.setParentModel(this);
		this.alias = alias;
		this.filter = filter;
		this.filter.setParentModel(this);
	}

	public QueryModel(ExpressionBuilderModel parent) {
		super(parent);
		this.source = new ExpressionBuilderModel(this);
		this.filter = new ExpressionBuilderModel(this); 
		this.sort = new QuerySortModel(this);
		this.relationship = new ExpressionBuilderModel(this);
		this.alias = "";
	}
	
	public ExpressionBuilderModel getSource() {
		return source;
	}
	
	public String getAlias() {
		return alias;
	}
	
	public void setAlias(String alias) {
		this.alias = alias;
	}

	public ExpressionBuilderModel getFilter() {
		return filter;
	}
	
	public QuerySortModel getSort() {
		return sort;
	}	
	
	public ExpressionBuilderModel getRelationship() {
		return relationship;
	}

	@Override
	public String getCQL(String indentation) {		
		StringBuilder builder = new StringBuilder();
		
		if(this.getParentModel().getParentModel() != null) {
			builder.append("(");
		}
						
		builder.append(source.getCQL(indentation + "  "));
				
		builder.append(" ");
		builder.append(alias);

		String queryContentIndentation = indentation + "  ";
		
		
		if (!relationship.getChildModels().isEmpty()) {
			 builder.append("\n").append(queryContentIndentation);
			 builder.append(relationship.getCQL(indentation + "  "));
		 }
				
		if (!filter.getChildModels().isEmpty()) {
			builder.append("\n").append(queryContentIndentation);
			builder.append("where ");
		}
		
		if(this.getChildModels().size() == 1) {
			builder.append(this.getChildModels().get(0).getCQL(queryContentIndentation));
		} else {
			if (!filter.getChildModels().isEmpty()) {
				builder.append(filter.getCQL(queryContentIndentation));
			}
		}

		if(!sort.getSortExpression().getChildModels().isEmpty()) {
			builder.append("\n").append(queryContentIndentation);
			builder.append(sort.getCQL(""));
		}
		
		if(this.getParentModel().getParentModel() != null) {
			builder.append(")");
		}
				
		return builder.toString();
	}

	@Override
	public CQLType getType() {
		return CQLType.LIST;
	}
	
	@Override
	public String getDisplayName() {
		return ExpressionType.QUERY.getDisplayName();
	}

	public String getRelationshipType() {
		return relationshipType;
	}

	public void setRelationshipType(String relationshipType) {
		this.relationshipType = relationshipType;
	}

}