---
name: jsp-ui
description: Skill for creating and modifying JSP views with JSTL, Tiles layout, and frontend integration
---

# JSP UI Skill

## Purpose

Guide the creation and modification of JSP views, Tiles layouts, and frontend code in the EFFORT-Web application.

## When to Use

- Creating new JSP pages
- Modifying existing views
- Adding JSTL logic to views
- Integrating with Apache Tiles layouts
- Working with JavaScript/jQuery in JSP pages

## Architecture Role

```
Controller (sets Model attributes) → View Resolver → Tiles Layout → JSP Page
```

Views receive data via Spring Model attributes set by controllers. Apache Tiles manages page layouts (header, sidebar, content, footer).

## Best Practices

### JSP Page Structure

```jsp
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>

<div class="container">
    <h2>${pageTitle}</h2>

    <c:if test="${not empty errorMessage}">
        <div class="alert alert-danger">${errorMessage}</div>
    </c:if>

    <table class="table table-striped">
        <thead>
            <tr>
                <th>Name</th>
                <th>Status</th>
                <th>Actions</th>
            </tr>
        </thead>
        <tbody>
            <c:forEach var="item" items="${itemList}">
                <tr>
                    <td><c:out value="${item.name}" /></td>
                    <td>${item.status}</td>
                    <td>
                        <a href="<c:url value='/web/edit/${item.id}' />" class="btn btn-sm btn-primary">Edit</a>
                    </td>
                </tr>
            </c:forEach>
        </tbody>
    </table>
</div>
```

### Enterprise Standards

- **Always use `<c:out>`** for user-generated content to prevent XSS
- **Use JSTL tags** (`<c:if>`, `<c:forEach>`, `<c:choose>`) — avoid scriptlets (`<% %>`)
- **Use `<c:url>`** for URL construction to ensure context path is included
- **Use Spring Security tags** (`<sec:authorize>`) for role-based UI elements
- **Follow Bootstrap 5** conventions for CSS classes (already in project)
- **Use jQuery** for AJAX calls and DOM manipulation (project standard)

### Tiles Layout Integration

Views are defined in `tiles-definitions.xml`. A typical definition:

```xml
<definition name="itemList" extends="defaultLayout">
    <put-attribute name="title" value="Item List" />
    <put-attribute name="body" value="/WEB-INF/views/itemList.jsp" />
</definition>
```

The controller returns the tiles definition name:
```java
return "itemList"; // matches the Tiles definition name
```

### AJAX Pattern with jQuery

```javascript
$.ajax({
    url: contextPath + '/web/saveItem',
    type: 'POST',
    data: { name: itemName, customerId: customerId },
    dataType: 'json',
    success: function(response) {
        if (response.status === 'success') {
            showNotification('Item saved successfully');
            reloadTable();
        } else {
            showNotification(response.message, 'error');
        }
    },
    error: function(xhr, status, error) {
        showNotification('An error occurred', 'error');
    }
});
```

### Security in JSP

- Use `<c:out>` to escape HTML output
- Use OWASP ESAPI encoder for contexts not covered by JSTL
- Include CSRF tokens in forms when required
- Use `<sec:authorize access="hasRole('ADMIN')">` to control visibility
