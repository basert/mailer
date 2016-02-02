package jubu.mailer

import javax.mail.internet.{MimeMultipart, InternetAddress}
import javax.mail.{Folder, Store, internet}

import de.saly.javamail.mock2.{MailboxFolder, MockMailbox}
import org.scalatest._

/**
	* Test suite used to test the basic functionality of the ''Mailer''.
	*
	* @author vaclav.svejcar
	*/
class MailerSpec extends FlatSpec with Matchers {

	val RecipientAddress = "receiver@test.com"
	val SenderAddress = "sender@test.com"
	val MessageSubject = "Test e-mail"
	val MessageContentText = "Test content - plaintext"
	val MessageContentHtml = "Test content - <b>HTML</b>"
	val SmtpHost = "localhost"
	val SmtpPort = 25

	"Session" should "parse set of properties and return correct value" in {
		val session = (SmtpAddress(SmtpHost, SmtpPort) :: Debug(true) :: SmtpTimeout(1000) :: SessionFactory()).session()
		assert(session.getProperties.size() == 4)
	}

	"The Mailer" should "send e-mail" in {
		val session = (SmtpAddress(SmtpHost, SmtpPort) :: SessionFactory()).session()

		// send e-mail using the 'javamail-mock2' mock
		val content = new Content().text(MessageContentText).html(MessageContentHtml)
		Mailer(session).send(Msg(
			from = new InternetAddress(SenderAddress),
			subject = MessageSubject,
			content = content,
			to = Seq(new internet.InternetAddress(RecipientAddress)
			)))

		// open the fake INBOX folder
		val store: Store = session.getStore("pop3s")
		store.connect(RecipientAddress, null)
		val inbox: Folder = store.getFolder("INBOX")
		inbox.open(Folder.READ_ONLY)

		// check whether the single sent message has arrived
		inbox.getMessageCount should be(1)

		val firstMessage = inbox.getMessage(1)
		val firstContent = firstMessage.getContent

		// check whether the e-mail metadata are correct
		firstMessage.getSubject should be(MessageSubject)
		firstMessage.getFrom()(0).toString should be(SenderAddress)
		firstMessage.getAllRecipients()(0).toString should be(RecipientAddress)

		// check whether the content parts in the MimeMultipart message are correct
		firstContent should be(an[MimeMultipart])

		firstContent match {
			case mm: MimeMultipart => {
				mm.getCount should be(2)
			}
		}
	}
}
