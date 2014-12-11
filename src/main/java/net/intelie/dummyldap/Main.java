package net.intelie.dummyldap;

import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmPartition;
import org.apache.directory.server.ldap.LdapService;
import org.apache.directory.server.protocol.shared.SocketAcceptor;
import org.apache.directory.server.protocol.shared.store.LdifFileLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;

public class Main {
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    public static void main(String... args) throws Exception {
        int port = 1389;
        String data = "ldapdata";
        if (args.length >= 1 && !"-".equals(args[0]))
            port = Integer.parseInt(args[0]);
        if (args.length >= 2 && !"-".equals(args[1]))
            data = args[1];

        LOG.info("Saving data at {}", data);

        boolean first = false;

        File workingDirectory = new File(data, "db");
        if (!workingDirectory.exists())
            first = true;

        workingDirectory.mkdirs();

        Partition partition = new JdbmPartition();
        partition.setId("ou=Users");
        partition.setSuffix("dc=example,dc=com");

        DirectoryService directory = new DefaultDirectoryService();
        directory.setWorkingDirectory(workingDirectory);
        directory.addPartition(partition);
        directory.setShutdownHookEnabled(false);

        LdapService server = new LdapService();
        server.setSocketAcceptor(new SocketAcceptor(null));
        server.setIpPort(port);
        server.setDirectoryService(directory);

        directory.startup();
        server.start();

        if (first) {
            File startup = new File(data, "startup.ldif");
            if (!startup.exists()) {
                startup.getParentFile().mkdirs();
                try (InputStream input = Main.class.getResourceAsStream("/test.ldif")) {
                    Files.copy(input, startup.toPath());
                }
            }
            LdifFileLoader ldif = new LdifFileLoader(directory.getAdminSession(), startup.getPath());
            ldif.execute();
        }

        LOG.info("*******************");
        LOG.info("ADMIN: uid=admin,ou=system (default pass: 'secret')");
        LOG.info("*******************");

    }

}
