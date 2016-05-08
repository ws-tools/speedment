/**
 *
 * Copyright (c) 2006-2016, Speedment, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); You may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.speedment.generator.internal;

import com.speedment.runtime.Speedment;
import com.speedment.generator.Translator;
import com.speedment.generator.TranslatorManager;
import com.speedment.fika.codegen.Generator;
import com.speedment.fika.codegen.Meta;
import com.speedment.fika.codegen.model.File;
import com.speedment.generator.component.CodeGenerationComponent;
import com.speedment.runtime.config.Project;
import com.speedment.runtime.config.Table;
import com.speedment.runtime.config.trait.HasEnabled;
import com.speedment.generator.event.AfterGenerate;
import com.speedment.generator.event.BeforeGenerate;
import com.speedment.runtime.exception.SpeedmentException;
import com.speedment.fika.codegen.internal.java.JavaGenerator;
import com.speedment.fika.codegen.internal.util.Formatting;
import com.speedment.fika.logger.Logger;
import com.speedment.fika.logger.LoggerManager;
import com.speedment.runtime.internal.util.Statistics;
import static com.speedment.runtime.internal.util.document.DocumentDbUtil.traverseOver;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import static java.util.Objects.requireNonNull;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import static java.util.stream.Collectors.toList;

/**
 *
 * @author pemi
 */
public class TranslatorManagerImpl implements TranslatorManager {

    private static final Logger LOGGER = LoggerManager.getLogger(TranslatorManagerImpl.class);
    private static final boolean PRINT_CODE = false;
    private final AtomicInteger fileCounter = new AtomicInteger(0);

    private final Speedment speedment;

    public TranslatorManagerImpl(Speedment speedment) {
        this.speedment = requireNonNull(speedment);
    }

    @Override
    public void accept(Project project) {
        requireNonNull(project);
        Statistics.onGenerate();

        final List<Translator<?, ?>> writeOnceTranslators = new ArrayList<>();
        final List<Translator<?, ?>> writeAlwaysTranslators = new ArrayList<>();
        final Generator gen = new JavaGenerator();

        fileCounter.set(0);
        Formatting.tab("    ");

        speedment.getEventComponent().notify(new BeforeGenerate(project, gen, this));

        final CodeGenerationComponent cgc = speedment.get(CodeGenerationComponent.class);

        cgc.translators(project)
            .forEachOrdered(t -> {
                if (t.isInGeneratedPackage()) {
                    writeAlwaysTranslators.add(t);
                } else {
                    writeOnceTranslators.add(t);
                }
            });

        traverseOver(project, Table.class)
            .filter(HasEnabled::test)
            .forEach(table -> {
                cgc.translators(table).forEachOrdered(t -> {
                    if (t.isInGeneratedPackage()) {
                        writeAlwaysTranslators.add(t);
                    } else {
                        writeOnceTranslators.add(t);
                    }
                });
            });

        gen.metaOn(writeOnceTranslators.stream()
            .map(Translator::get)
            .collect(Collectors.toList())
        ).forEach(meta -> writeToFile(project, meta, false));

        gen.metaOn(writeAlwaysTranslators.stream()
            .map(Translator::get)
            .collect(Collectors.toList())
        ).forEach(meta -> writeToFile(project, meta, true));

        final List<Table> tables = traverseOver(project, Table.class)
            .filter(HasEnabled::test)
            .collect(toList());

        gen.metaOn(tables, File.class).forEach(meta -> {
            writeToFile(project, gen, meta);
            fileCounter.incrementAndGet();
        });

        speedment.getEventComponent().notify(new AfterGenerate(project, gen, this));
    }

    @Override
    public int getFilesCreated() {
        return fileCounter.get();
    }

    @Override
    public void writeToFile(Project project, Meta<File, String> meta, boolean overwriteExisting) {
        requireNonNull(meta);

        final String fname = project.getPackageLocation()
            + "/"
            + meta.getModel().getName();
        final String content = meta.getResult();
        final Path path = Paths.get(fname);
        try {
            final boolean created = Optional.ofNullable(path.getParent()).map(p -> p.toFile().mkdirs()).orElse(false);
        } catch (SecurityException se) {
            throw new SpeedmentException("Unable to create directory " + path.toString(), se);
        }

        try {
            if (overwriteExisting || !path.toFile().exists()) {
                Files.write(path, content.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
                );
                fileCounter.incrementAndGet();
            }
        } catch (final IOException ex) {
            LOGGER.error(ex, "Failed to write file " + fname);
        }

        //LOGGER.info("done");

        printCode(fname, content);

        if (PRINT_CODE) {
            System.out.println("*** BEGIN File:" + fname);
            System.out.println(content);
            System.out.println("*** END   File:" + fname);
        }
    }

    public static void writeToFile(Project project, Generator gen, Meta<Table, File> meta) {
        requireNonNull(project);
        requireNonNull(gen);
        requireNonNull(meta);

        final Optional<String> content = gen.on(meta.getResult());

        if (content.isPresent()) {
            final String fname
                = project.getPackageLocation()
                + "/" + meta.getResult().getName();

            final Path path = Paths.get(fname);
            try {
                final boolean created = Optional.ofNullable(path.getParent()).map(p -> p.toFile().mkdirs()).orElse(false);
            } catch (SecurityException se) {
                throw new SpeedmentException("Unable to create directory " + path.toString(), se);
            }
            try {
                Files.write(path,
                    content.get().getBytes(StandardCharsets.UTF_8)
                );
            } catch (IOException ex) {
                LOGGER.error(ex, "Failed to create file " + fname);
            }

            LOGGER.info("done");

            printCode(fname, content.get());

        } else {
            throw new IllegalArgumentException("Input file could not be generated.");
        }
    }

    private static void printCode(final String fname, final String content) {
        if (PRINT_CODE) {
            System.out.println("*** BEGIN File:" + fname);
            System.out.println(content);
            System.out.println("*** END   File:" + fname);
        }
    }

}
