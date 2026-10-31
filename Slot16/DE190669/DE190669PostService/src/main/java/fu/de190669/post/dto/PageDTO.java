package fu.de190669.post.dto;

import org.springframework.data.domain.Page;

public class PageDTO {
    private int size;
    private int page;
    private int totalPages;
    private long totalElements;
    private boolean first;
    private boolean last;
    private Object content;

    public PageDTO(Page<?> source, Object content) {
        this.size = source.getSize();
        this.page = source.getNumber();
        this.totalPages = source.getTotalPages();
        this.totalElements = source.getTotalElements();
        this.first = source.isFirst();
        this.last = source.isLast();
        this.content = content;
    }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
    public long getTotalElements() { return totalElements; }
    public void setTotalElements(long totalElements) { this.totalElements = totalElements; }
    public boolean isFirst() { return first; }
    public void setFirst(boolean first) { this.first = first; }
    public boolean isLast() { return last; }
    public void setLast(boolean last) { this.last = last; }
    public Object getContent() { return content; }
    public void setContent(Object content) { this.content = content; }
}
