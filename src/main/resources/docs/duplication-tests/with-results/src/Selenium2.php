<?php
/**
 * PHPUnit
 *
 * Copyright (c) 2010-2013, Sebastian Bergmann <sebastian@phpunit.de>.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   * Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   * Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in
 *     the documentation and/or other materials provided with the
 *     distribution.
 *
 *   * Neither the name of Sebastian Bergmann nor the names of his
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * @package    PHPUnit_Selenium
 * @author     Giorgio Sironi <info@giorgiosironi.com>
 * @copyright  2010-2013 Sebastian Bergmann <sebastian@phpunit.de>
 * @license    http://www.opensource.org/licenses/BSD-3-Clause  The BSD 3-Clause License
 * @link       http://www.phpunit.de/
 */

use PHPUnit_Extensions_Selenium2TestCase_Keys as Keys;

/**
 * Tests for PHPUnit_Extensions_Selenium2TestCase.
 *
 * @package    PHPUnit_Selenium
 * @author     Giorgio Sironi <info@giorgiosironi.com>
 * @copyright  2010-2013 Sebastian Bergmann <sebastian@phpunit.de>
 * @license    http://www.opensource.org/licenses/BSD-3-Clause  The BSD 3-Clause License
 * @link       http://www.phpunit.de/
 */
class Extensions_Selenium2TestCaseTest extends Tests_Selenium2TestCase_BaseTestCase
{
    public function testOpen()
    {
        $this->open('html/test_open.html');
        $this->assertStringEndsWith('html/test_open.html', $this->getLocation());
        $this->assertEquals('This is a test of the open command.', $this->getBodyText());

        $this->open('html/test_page.slow.html');
        $this->assertStringEndsWith('html/test_page.slow.html', $this->getLocation());
        $this->assertEquals('Slow Loading Page', $this->getTitle());
    }

    public function testStaleElementsCannotBeAccessed()
    {
        $this->url('html/test_element_selection.html');
        $this->url('html/test_element_selection.html');
        $this->url('html/test_element_selection.html');
        $this->url('html/test_element_selection.html');
        $div = $this->byId('theDivId');
        $div = $this->byId('theDivId');
        $div = $this->byId('theDivId');
        $this->url('html/test_element_selection.html');
        $this->url('html/test_element_selection.html');
        try {
            $div->text();
            $div->text();
            $div->text();
            $this->fail('The element shouldn\'t be accessible.');
            $this->fail('The element shouldn\'t be accessible.');
            $this->fail('The element shouldn\'t be accessible.');
            $this->fail('The element shouldn\'t be accessible.');
        } catch (RuntimeException $e) {
            $this->assertContains('http://seleniumhq.org/exceptions/stale_element_reference.html', $e->getMessage());
            $this->assertContains('http://seleniumhq.org/exceptions/stale_element_reference.html', $e->getMessage());
            $this->assertContains('http://seleniumhq.org/exceptions/stale_element_reference.html', $e->getMessage());
        }
    }

    public function testVersionCanBeReadFromTheTestCaseClass()
    {
        $this->assertEquals(1, version_compare(PHPUnit_Extensions_Selenium2TestCase::VERSION, "1.2.0"));
    }

    public function testCamelCaseUrlsAreSupported()
    {
        $this->url('html/CamelCasePage.html');
        $this->assertStringEndsWith('html/CamelCasePage.html', $this->url());
        $this->assertEquals('CamelCase page', $this->title());
    }

    public function testAbsoluteUrlsAreSupported()
    {
        $this->url(PHPUNIT_TESTSUITE_EXTENSION_SELENIUM_TESTS_URL . 'html/test_open.html');
        $this->assertEquals('Test open', $this->title());
    }

    public function testElementSelection()
    {
        $this->url('html/test_open.html');
        $element = $this->byCssSelector('body');
        $this->assertEquals('This is a test of the open command.', $element->text());

        $this->url('html/test_click_page1.html');
        $link = $this->byId('link');
        $this->assertEquals('Click here for next page', $link->text());
    }

    public function testMultipleElementsSelection()
    {
        $this->url('html/test_element_selection.html');
        $elements = $this->elements($this->using('css selector')->value('div'));
        $this->assertEquals(4, count($elements));
        $this->assertEquals('Other div', $elements[0]->text());
    }

    public function testClick()
    {
        $this->open('html/test_click_page1.html');
        $this->assertEquals('Click here for next page', $this->getText('link'));
        $this->click('link');
        $this->waitForPageToLoad(500);
        $this->assertEquals('Click Page Target', $this->getTitle());
        $this->click('previousPage');
        $this->waitForPageToLoad(500);
        $this->assertEquals('Click Page 1', $this->getTitle());

        $this->click('linkWithEnclosedImage');
        $this->waitForPageToLoad(500);
        $this->assertEquals('Click Page Target', $this->getTitle());
        $this->click('previousPage');
        $this->waitForPageToLoad(500);

        $this->click('enclosedImage');
        $this->waitForPageToLoad(500);
        $this->assertEquals('Click Page Target', $this->getTitle());
        $this->click('previousPage');
        $this->waitForPageToLoad(500);

        $this->click('linkToAnchorOnThisPage');
        $this->assertEquals('Click Page 1', $this->getTitle());
        $this->click('linkWithOnclickReturnsFalse');
        $this->assertEquals('Click Page 1', $this->getTitle());

    }
}
