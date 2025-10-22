package dev.lucas.user_microservice.dtos;

public class EmailDto {

    private Long userId;
    private String emailTo;
    private String subject;
    private String text;

    //NoArgs Constructor
    public EmailDto() {
    }

    //AllArgs Constructor
    public EmailDto(String text, String subject, String emailTo, Long userId) {
        this.text = text;
        this.subject = subject;
        this.emailTo = emailTo;
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }


    public String getEmailTo() {
        return emailTo;
    }

    public void setEmailTo(String emailTo) {
        this.emailTo = emailTo;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}