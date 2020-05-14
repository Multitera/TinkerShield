package com.multitera.tinkershield.book;

import slimeknights.mantle.client.book.BookTransformer;
import slimeknights.mantle.client.book.data.BookData;
import slimeknights.mantle.client.book.data.PageData;
import slimeknights.mantle.client.book.data.SectionData;
import slimeknights.tconstruct.library.TinkerRegistry;
import slimeknights.tconstruct.library.book.content.ContentListing;
import slimeknights.tconstruct.library.book.content.ContentTool;
import slimeknights.tconstruct.library.modifiers.IModifier;

public class BattleShieldBookTransformer extends BookTransformer {

    @Override
    public void transform(BookData book) {

        SectionData toolsSection = null;
        SectionData shieldToolSection = null;
        for (SectionData section : book.sections) {
            if (section.name.equals("tools")) {
                toolsSection = section;
            }
            if (section.name.equals("tinkershield_tools")) {
                shieldToolSection = section;
            }
        }
        if (toolsSection != null && shieldToolSection != null) {
            for (PageData page : shieldToolSection.pages) {
                page.parent = toolsSection;
                toolsSection.pages.add(page);
            }
            PageData pageData = toolsSection.pages.get(0);
            if (pageData.content instanceof ContentListing) {
                for (PageData page : shieldToolSection.pages) {
                    page.parent = toolsSection;
                    if (page.content instanceof ContentTool) {
                        IModifier tool = TinkerRegistry.getModifier(((ContentTool) page.content).toolName);
                        if (tool != null) {
                            page.name = "tinkershield_" + tool.getIdentifier();
                            ((ContentListing) pageData.content).addEntry(tool.getLocalizedName(), page);
                        }
                    }
                }
            }
            shieldToolSection.pages.clear();
            book.sections.remove(shieldToolSection);
        }
    }
}
