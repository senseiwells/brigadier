package com.mojang.brigadier;

import com.google.common.collect.Lists;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.tree.LiteralCommandNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static com.mojang.brigadier.builder.LiteralArgumentBuilder.literal;
import static com.mojang.brigadier.builder.RequiredArgumentBuilder.argument;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class CommandSuggestionsTest {
    private CommandDispatcher<Object> subject;
    @Mock
    private Object source;

    @Before
    public void setUp() throws Exception {
        subject = new CommandDispatcher<>();
    }

    @Test
    public void getCompletionSuggestions_rootCommands() throws Exception {
        subject.register(literal("foo"));
        subject.register(literal("bar"));
        subject.register(literal("baz"));

        final CommandSuggestions result = subject.getCompletionSuggestions(subject.parse("", source)).join();

        assertThat(result.getRange(), equalTo(new StringRange(0, 0)));
        assertThat(result.getSuggestions(), equalTo(Lists.newArrayList("bar", "baz", "foo")));
    }

    @Test
    public void getCompletionSuggestions_rootCommands_partial() throws Exception {
        subject.register(literal("foo"));
        subject.register(literal("bar"));
        subject.register(literal("baz"));

        final CommandSuggestions result = subject.getCompletionSuggestions(subject.parse("b", source)).join();

        assertThat(result.getRange(), equalTo(new StringRange(0, 1)));
        assertThat(result.getSuggestions(), equalTo(Lists.newArrayList("bar", "baz")));
    }

    @Test
    public void getCompletionSuggestions_subCommands() throws Exception {
        subject.register(
            literal("parent")
                .then(literal("foo"))
                .then(literal("bar"))
                .then(literal("baz"))
        );

        final CommandSuggestions result = subject.getCompletionSuggestions(subject.parse("parent ", source)).join();

        assertThat(result.getRange(), equalTo(new StringRange(7, 7)));
        assertThat(result.getSuggestions(), equalTo(Lists.newArrayList("bar", "baz", "foo")));
    }

    @Test
    public void getCompletionSuggestions_subCommands_partial() throws Exception {
        subject.register(
            literal("parent")
                .then(literal("foo"))
                .then(literal("bar"))
                .then(literal("baz"))
        );

        final ParseResults<Object> parse = subject.parse("parent b", source);
        final CommandSuggestions result = subject.getCompletionSuggestions(parse).join();

        assertThat(result.getRange(), equalTo(new StringRange(7, 8)));
        assertThat(result.getSuggestions(), equalTo(Lists.newArrayList("bar", "baz")));
    }

    @Test
    public void getCompletionSuggestions_redirect() throws Exception {
        final LiteralCommandNode<Object> actual = subject.register(literal("actual").then(literal("sub")));
        subject.register(literal("redirect").redirect(actual));

        final ParseResults<Object> parse = subject.parse("redirect ", source);
        final CommandSuggestions result = subject.getCompletionSuggestions(parse).join();

        assertThat(result.getRange(), equalTo(new StringRange(9, 9)));
        assertThat(result.getSuggestions(), equalTo(Lists.newArrayList("sub")));
    }

    @Test
    public void getCompletionSuggestions_redirectPartial() throws Exception {
        final LiteralCommandNode<Object> actual = subject.register(literal("actual").then(literal("sub")));
        subject.register(literal("redirect").redirect(actual));

        final ParseResults<Object> parse = subject.parse("redirect s", source);
        final CommandSuggestions result = subject.getCompletionSuggestions(parse).join();

        assertThat(result.getRange(), equalTo(new StringRange(9, 10)));
        assertThat(result.getSuggestions(), equalTo(Lists.newArrayList("sub")));
    }

    @Test
    public void getCompletionSuggestions_redirect_lots() throws Exception {
        final LiteralCommandNode<Object> loop = subject.register(literal("redirect"));
        subject.register(
            literal("redirect")
                .then(
                    literal("loop")
                        .then(
                            argument("loop", integer())
                                .redirect(loop)
                        )
                )
        );

        final CommandSuggestions result = subject.getCompletionSuggestions(subject.parse("redirect loop 1 loop 02 loop 003 ", source)).join();

        assertThat(result.getRange(), equalTo(new StringRange(33, 33)));
        assertThat(result.getSuggestions(), equalTo(Lists.newArrayList("loop")));
    }

    @Test
    public void getCompletionSuggestions_execute_simulation() throws Exception {
        final LiteralCommandNode<Object> execute = subject.register(literal("execute"));
        subject.register(
            literal("execute")
                .then(
                    literal("as")
                        .then(
                            argument("name", word())
                                .redirect(execute)
                        )
                )
                .then(
                    literal("store")
                        .then(
                            argument("name", word())
                                .redirect(execute)
                        )
                )
                .then(
                    literal("run")
                        .executes(c -> 0)
                )
        );

        final ParseResults<Object> parse = subject.parse("execute as Dinnerbone as", source);
        final CommandSuggestions result = subject.getCompletionSuggestions(parse).join();

        assertThat(result.getRange(), equalTo(new StringRange(22, 24)));
        assertThat(result.getSuggestions(), equalTo(Lists.newArrayList("as")));
    }

    @Test
    public void getCompletionSuggestions_execute_simulation_partial() throws Exception {
        final LiteralCommandNode<Object> execute = subject.register(literal("execute"));
        subject.register(
            literal("execute")
                .then(
                    literal("as")
                        .then(literal("bar").redirect(execute))
                        .then(literal("baz").redirect(execute))
                )
                .then(
                    literal("store")
                        .then(
                            argument("name", word())
                                .redirect(execute)
                        )
                )
                .then(
                    literal("run")
                        .executes(c -> 0)
                )
        );

        final ParseResults<Object> parse = subject.parse("execute as bar as ", source);
        final CommandSuggestions result = subject.getCompletionSuggestions(parse).join();

        assertThat(result.getRange(), equalTo(new StringRange(18, 18)));
        assertThat(result.getSuggestions(), equalTo(Lists.newArrayList("bar", "baz")));
    }
}