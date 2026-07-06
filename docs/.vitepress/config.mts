import {defineConfig} from 'vitepress';

export default defineConfig({
    title: 'Minecraft Mods',
    description: 'Documentation for Oliver Yasuna\'s Minecraft mods and libraries.',
    cleanUrls: true,

    themeConfig: {
        nav: [
            {text: 'Home', link: '/'},
            {text: 'COAL', link: '/coal/'},
        ],

        sidebar: {
            '/coal/': [
                {
                    text: 'COAL',
                    items: [
                        {text: 'Overview', link: '/coal/'},
                        {text: 'Introduction', link: '/coal/introduction'},
                        {text: 'Getting started', link: '/coal/getting-started'},
                    ],
                },
                {
                    text: 'Concepts',
                    collapsed: false,
                    items: [
                        {text: 'Provider model', link: '/coal/concepts/provider-model'},
                        {text: 'Capabilities', link: '/coal/concepts/capabilities'},
                        {text: 'GUI delegation', link: '/coal/concepts/gui-delegation'},
                        {text: 'Lifecycle and events', link: '/coal/concepts/lifecycle-and-events'},
                        {text: 'Server ↔ client sync', link: '/coal/concepts/sync'},
                    ],
                },
                {
                    text: 'Guides',
                    collapsed: false,
                    items: [
                        {text: 'Define a config', link: '/coal/guides/defining-a-config'},
                        {text: 'Handle load corrections', link: '/coal/guides/handling-load-corrections'},
                        {text: 'Migrate configs', link: '/coal/guides/migrating-configs'},
                        {text: 'Write a provider', link: '/coal/guides/writing-a-provider'},
                        {text: 'Conformance-test a provider', link: '/coal/guides/conformance-testing'},
                    ],
                },
                {
                    text: 'Reference',
                    collapsed: false,
                    items: [
                        {text: 'Annotations', link: '/coal/reference/annotations'},
                        {text: 'SPI', link: '/coal/reference/spi'},
                        {text: 'Specification', link: '/coal/spec/'},
                    ],
                },
                {
                    text: 'Adapters',
                    collapsed: false,
                    items: [
                        {text: 'YACL', link: '/coal/adapters/yacl'},
                        {text: 'Cloth Config', link: '/coal/adapters/cloth'},
                        {text: 'Noop', link: '/coal/adapters/noop'},
                    ],
                },
            ],
        },

        socialLinks: [
            {icon: 'github', link: 'https://github.com/oliveryasuna/minecraft-mods'},
        ],
    },
});
