/**
 * Copyright (c) 2010-2012 Grid Dynamics Consulting Services, Inc, All Rights Reserved
 * http://www.griddynamics.com
 *
 * This library is free software; you can redistribute it and/or modify it under the terms of
 * the GNU Lesser General Public License as published by the Free Software Foundation; either
 * version 2.1 of the License, or any later version.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Project:     Genesis
 * Description: Continuous Delivery Platform
 */
package com.griddynamics.genesis.steps.builder

import org.junit.Test
import com.griddynamics.genesis.workflow.Step

class ReflectionBasedStepBuilderTest {

  @Test def shouldBuildNormallyIfAllParametersProvided() {
    val builder = new ReflectionBasedStepBuilder(classOf[BasicCaseClass])
    builder.setProperty("intValue", 5)
    builder.setProperty("stringValue", "some string")
    builder.setProperty("seq", java.util.Arrays.asList(1, 2, 3))
    val result = builder.getDetails
    assert(result == new BasicCaseClass(5, "some string", Seq(1, 2, 3)))
  }

  @Test def itShouldBePossibleToHaveEmptyDefaultsForSequences() {
    val builder = new ReflectionBasedStepBuilder(classOf[BasicCaseClass])
    builder.setProperty("intValue", 5)
    builder.setProperty("stringValue", "some string")
    val result = builder.getDetails
    assert(result == new BasicCaseClass(5, "some string", Seq()))
  }

  @Test(expected = classOf[Exception]) //todo exception type is awkward
  def shouldThrowExceptionIfMandatoryParametersAreAbsent() {
    val builder = new ReflectionBasedStepBuilder(classOf[BasicCaseClass])
    builder.setProperty("intValue", 5)
    builder.getDetails
  }

  @Test(expected = classOf[IllegalArgumentException])
  def shouldThrowExceptionIfListContainsWrongType() {
    val builder = new ReflectionBasedStepBuilder(classOf[BasicCaseClass])
    builder.setProperty("intValue", 5)
    builder.setProperty("stringValue", "some string")
    builder.setProperty("seq", java.util.Arrays.asList("uno", "dos", "tres"))
    builder.getDetails
  }

  @Test def shouldWrapOptionIfProvided() {
    val builder = new ReflectionBasedStepBuilder(classOf[AdvancedCaseClass])
    builder.setProperty("intValue", 5)
    builder.setProperty("opt", "some string")
    val value = builder.getDetails
    assert(value == new AdvancedCaseClass(5, Some("some string")))
  }

  @Test def shouldUseNoneIfOptionalIsNotProvided() {
    val builder = new ReflectionBasedStepBuilder(classOf[AdvancedCaseClass])
    builder.setProperty("intValue", 5)
    val value = builder.getDetails
    assert(value == new AdvancedCaseClass(5, None))
  }

  @Test def shouldWrapNullToOption() {
    val builder = new ReflectionBasedStepBuilder(classOf[AdvancedCaseClass])
    val value = builder.getDetails
    assert(value == new AdvancedCaseClass(666, None))
  }

  @Test def shouldUseDefaultConstructorValue() {
    val builder = new ReflectionBasedStepBuilder(classOf[AdvancedCaseClass])
    builder.setProperty("opt", null)
    val value = builder.getDetails
    assert(value == new AdvancedCaseClass(666, None))
  }

  @Test(expected = classOf[IllegalArgumentException])
  def optionShouldRespectType() {
    val builder = new ReflectionBasedStepBuilder(classOf[AdvancedCaseClass])
    builder.setProperty("opt", 999)
    builder.getDetails
  }

  @Test def weirdCase() {
    val builder = new ReflectionBasedStepBuilder(classOf[OptionSeqCaseClass])
    builder.setProperty("list", new java.util.ArrayList[Int]() {
      {
        add(1); add(2); add(3);
      }
    })
    val result = builder.getDetails
    assert(result == OptionSeqCaseClass(Some(Seq(1, 2, 3))))
  }
}

case class BasicCaseClass(intValue: Int, stringValue: String, seq: Seq[Int]) extends Step

case class AdvancedCaseClass(intValue: Int = 666, opt: Option[String]) extends Step

case class OptionSeqCaseClass(list: Option[Seq[Int]]) extends Step
