/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.codahale.usl4j.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codahale.usl4j.Measurement;
import com.codahale.usl4j.Model;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;
import org.assertj.core.data.Offset;
import org.assertj.core.data.Percentage;
import org.junit.Test;

public class ModelTest {

  static final Offset<Double> EPSILON = Offset.offset(0.00001);

  // data of Cisco benchmark from Practical Scalability by Baron Schwartz
  private static final double[][] CISCO = {
    {1, 955.16},
    {2, 1878.91},
    {3, 2688.01},
    {4, 3548.68},
    {5, 4315.54},
    {6, 5130.43},
    {7, 5931.37},
    {8, 6531.08},
    {9, 7219.8},
    {10, 7867.61},
    {11, 8278.71},
    {12, 8646.7},
    {13, 9047.84},
    {14, 9426.55},
    {15, 9645.37},
    {16, 9897.24},
    {17, 10097.6},
    {18, 10240.5},
    {19, 10532.39},
    {20, 10798.52},
    {21, 11151.43},
    {22, 11518.63},
    {23, 11806},
    {24, 12089.37},
    {25, 12075.41},
    {26, 12177.29},
    {27, 12211.41},
    {28, 12158.93},
    {29, 12155.27},
    {30, 12118.04},
    {31, 12140.4},
    {32, 12074.39}
  };
  // listed values of the fitted model from the book
  private static final double BOOK_KAPPA = 7.690945E-4;
  private static final double BOOK_SIGMA = 0.02671591;
  private static final double BOOK_LAMBDA = 995.6486;
  private static final double BOOK_N_MAX = 35;
  private static final double BOOK_X_MAX = 12341;
  // assert that the actual value is within 0.02% of the expected value
  private static final Percentage BOOK_TOLERANCE = Percentage.withPercentage(0.02);

  // a model built from the Cisco measurements
  private final Model model =
      Arrays.stream(CISCO).map(Measurement.ofConcurrency()::andThroughput).collect(Model.toModel());

  @Test
  public void minMeasurements() {
    assertThatThrownBy(() -> Model.build(Collections.emptyList()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void build() {
    final Model other =
        Model.build(
            Arrays.stream(CISCO)
                .map(Measurement.ofConcurrency()::andThroughput)
                .collect(Collectors.toList()));
    assertThat(model.sigma()).isCloseTo(other.sigma(), EPSILON);
  }

  @Test
  public void sigma() {
    assertThat(model.sigma()).isCloseTo(BOOK_SIGMA, BOOK_TOLERANCE);
  }

  @Test
  public void kappa() {
    assertThat(model.kappa()).isCloseTo(BOOK_KAPPA, BOOK_TOLERANCE);
  }

  @Test
  public void lambda() {
    assertThat(model.lambda()).isCloseTo(BOOK_LAMBDA, BOOK_TOLERANCE);
  }

  @Test
  public void maxConcurrency() {
    assertThat(model.maxConcurrency()).isCloseTo(BOOK_N_MAX, BOOK_TOLERANCE);
  }

  @Test
  public void maxThroughput() {
    assertThat(model.maxThroughput()).isCloseTo(BOOK_X_MAX, BOOK_TOLERANCE);
  }

  @Test
  public void coherency() {
    assertThat(model.isCoherencyConstrained()).isFalse();
  }

  @Test
  public void contention() {
    assertThat(model.isContentionConstrained()).isTrue();
  }

  @Test
  public void latencyAtConcurrency() {
    assertThat(model.latencyAtConcurrency(1)).isCloseTo(0.0010043984982923623, EPSILON);
    assertThat(model.latencyAtConcurrency(20)).isCloseTo(0.0018077217982978785, EPSILON);
    assertThat(model.latencyAtConcurrency(35)).isCloseTo(0.0028359135486017784, EPSILON);
  }

  @Test
  public void throughputAtConcurrency() {
    assertThat(model.throughputAtConcurrency(1)).isCloseTo(995.648772003358, EPSILON);
    assertThat(model.throughputAtConcurrency(20)).isCloseTo(11063.633137626028, EPSILON);
    assertThat(model.throughputAtConcurrency(35)).isCloseTo(12341.7456205207, EPSILON);
  }

  @Test
  public void concurrencyAtThroughput() {
    assertThat(model.concurrencyAtThroughput(955)).isCloseTo(0.9580998829620233, EPSILON);
    assertThat(model.concurrencyAtThroughput(11048)).isCloseTo(15.350435172752203, EPSILON);
    assertThat(model.concurrencyAtThroughput(12201)).isCloseTo(17.73220762025387, EPSILON);
  }

  @Test
  public void throughputAtLatency() {
    final Model model = Model.of(0.06, 0.06, 40);
    assertThat(model.throughputAtLatency(0.03)).isCloseTo(69.38886664887109, EPSILON);
    assertThat(model.throughputAtLatency(0.04)).isCloseTo(82.91561975888501, EPSILON);
    assertThat(model.throughputAtLatency(0.05)).isCloseTo(84.06346808612327, EPSILON);
  }

  @Test
  public void latencyAtThroughput() {
    final Model model = Model.of(0.06, 0.06, 40);
    assertThat(model.latencyAtThroughput(400)).isCloseTo(0.05875, EPSILON);
    assertThat(model.latencyAtThroughput(500)).isCloseTo(0.094, EPSILON);
    assertThat(model.latencyAtThroughput(600)).isCloseTo(0.235, EPSILON);
  }

  @Test
  public void concurrencyAtLatency() {
    // going off page 30-31
    final Model model =
        Arrays.stream(CISCO)
            .limit(10)
            .map(Measurement.ofConcurrency()::andThroughput)
            .collect(Model.toModel());
    assertThat(model.concurrencyAtLatency(0.0012)).isCloseTo(7.230628979597649, EPSILON);
    assertThat(model.concurrencyAtLatency(0.0016)).isCloseTo(20.25106409917121, EPSILON);
    assertThat(model.concurrencyAtLatency(0.0020)).isCloseTo(29.888882633013246, EPSILON);
  }

  @Test
  public void limitless() {
    final Model unlimited = Model.of(1, 0, 40);
    assertThat(unlimited.isLimitless()).isTrue();
    assertThat(model.isLimitless()).isFalse();
  }
}
