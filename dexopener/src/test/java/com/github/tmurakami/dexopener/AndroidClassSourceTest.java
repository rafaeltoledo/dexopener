package com.github.tmurakami.dexopener;

import com.github.tmurakami.classinjector.ClassFile;
import com.github.tmurakami.classinjector.ClassSource;
import com.github.tmurakami.dexopener.repackaged.org.jf.dexlib2.Opcodes;
import com.github.tmurakami.dexopener.repackaged.org.jf.dexlib2.immutable.ImmutableClassDef;
import com.github.tmurakami.dexopener.repackaged.org.jf.dexlib2.writer.io.FileDataStore;
import com.github.tmurakami.dexopener.repackaged.org.jf.dexlib2.writer.pool.DexPool;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.reflect.Modifier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.BDDMockito.given;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class AndroidClassSourceTest {

    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();

    @Mock
    private ClassNameFilter classNameFilter;
    @Mock
    private DexFilesFactory dexFilesFactory;
    @Mock
    private DexClassSourceFactory dexClassSourceFactory;
    @Mock
    private DexFiles dexFiles;
    @Mock
    private ClassSource classSource;
    @Mock
    private ClassFile classFile;

    @Captor
    private ArgumentCaptor<byte[]> bytecodeCaptor;

    @SuppressWarnings("TryFinallyCanBeTryWithResources")
    @Test
    public void should_get_the_class_file_for_the_given_name() throws Exception {
        String className = "foo.Bar";
        DexPool pool = new DexPool(Opcodes.getDefault());
        pool.internClass(new ImmutableClassDef(TypeUtils.getInternalName(className),
                                               Modifier.FINAL,
                                               null,
                                               null,
                                               null,
                                               null,
                                               null,
                                               null));
        File tmp = folder.newFile();
        pool.writeTo(new FileDataStore(tmp));
        byte[] bytecode;
        BufferedInputStream in = new BufferedInputStream(new FileInputStream(tmp));
        try {
            bytecode = IOUtils.readBytes(in);
        } finally {
            in.close();
        }
        File apk = folder.newFile();
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(apk));
        try {
            out.putNextEntry(new ZipEntry("classes.dex"));
            out.write(bytecode);
        } finally {
            out.close();
        }
        given(classNameFilter.accept(className)).willReturn(true);
        given(dexFilesFactory.newDexFiles(bytecodeCaptor.capture())).willReturn(dexFiles);
        given(dexClassSourceFactory.newClassSource(dexFiles)).willReturn(classSource);
        given(classSource.getClassFile(className)).willReturn(classFile);
        assertSame(classFile, new AndroidClassSource(apk.getCanonicalPath(),
                                                     classNameFilter,
                                                     dexFilesFactory,
                                                     dexClassSourceFactory).getClassFile(className));
        assertArrayEquals(bytecode, bytecodeCaptor.getValue());
    }

    @Test
    public void should_get_null_if_the_given_name_does_not_pass_through_the_filter()
            throws Exception {
        AndroidClassSource classSource = new AndroidClassSource("",
                                                                classNameFilter,
                                                                dexFilesFactory,
                                                                dexClassSourceFactory);
        assertNull(classSource.getClassFile("foo.Bar"));
    }

}
