#!/usr/bin/perl

use strict;
use warnings;
use LWP::UserAgent;
use Getopt::Long;
use Parallel::ForkManager;
use Time::HiRes qw(time);
use Net::Ping;
use IO::Socket::INET;

# COMMAND LINE OPTIONS
my $timeout = 5;
my $max_proxies = 10;
my $threads = 5;
my $ping_timeout = 3;
my $verbose = 0;
my $input_file = "";

GetOptions(
    "timeout=i" => \$timeout,
    "max=i"     => \$max_proxies,
    "threads=i" => \$threads,
    "ping=i"    => \$ping_timeout,
    "verbose"   => \$verbose,
    "file=s"    => \$input_file,
    "help"      => sub { print_help(); exit 0; }
);

# TEST URLS AND EXPECTED RESPONSES
my @TEST_URLS = (
    { url => "http://httpbin.org/ip", type => "HTTP", expected => qr/"origin"/ },
    { url => "https://httpbin.org/ip", type => "HTTPS", expected => qr/"origin"/ },
    { url => "http://icanhazip.com", type => "HTTP_SIMPLE", expected => qr/^\d+\.\d+\.\d+\.\d+/ },
    { url => "https://icanhazip.com", type => "HTTPS_SIMPLE", expected => qr/^\d+\.\d+\.\d+\.\d+/ }
);

# PARALLEL PROCESSING SETUP
my @proxies_to_test;

if ($input_file) {
    if (-e $input_file) {
        open(my $fh, '<', $input_file) or die "Could not open file '$input_file' $!";
        while (my $line = <$fh>) {
            # Extract IP:PORT using regex from any text (env file, raw list, etc)
            while ($line =~ /(\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}:\d+)/g) {
                push @proxies_to_test, $1;
            }
        }
        close $fh;
    } else {
        print STDERR "Warning: Input file '$input_file' not found.\n";
    }
} else {
    @proxies_to_test = <STDIN>;
    chomp(@proxies_to_test);
}

print STDERR "PROXY CHECKER v1.1\n";
print STDERR "━" x 50 . "\n";
print STDERR "   Testing " . scalar(@proxies_to_test) . " proxies with $threads threads\n";
print STDERR "   Timeout: ${timeout}s | Ping: ${ping_timeout}s\n";
print STDERR "   Max working proxies: $max_proxies\n";
print STDERR "━" x 50 . "\n";

my $pm = Parallel::ForkManager->new($threads);
my $found_count = 0;
my $total_tested = 0;

# Results callback
$pm->run_on_finish(sub {
    my ($pid, $exit_code, $ident, $exit_signal, $core_dump, $data) = @_;
    $total_tested++;

    if (defined $data && $data->{status} eq 'WORKING') {
        return if $found_count >= $max_proxies;
        $found_count++;

        my $proxy = $data->{proxy};
        my $tests = $data->{tests};
        my $ping = $data->{ping};
        my $location = $data->{location} || "Unknown";

        # Output working proxy with test results
        print "$proxy|$ping|$location|" . join(",", @$tests) . "\n";
        STDOUT->flush();

        print STDERR "[$found_count/$max_proxies] $proxy (${ping}ms) [$location] - " . join(", ", @$tests) . "\n" if $verbose;
    } elsif ($verbose && defined $data) {
        my $reason = $data->{reason} || "Unknown error";
        print STDERR "$data->{proxy} - $reason\n";
    }

    # Progress indicator
    if ($total_tested % 10 == 0) {
        my $progress = int(($total_tested / scalar(@proxies_to_test)) * 100);
        print STDERR "Progress: $progress% ($total_tested/" . scalar(@proxies_to_test) . ") | Found: $found_count\n";
    }
});

# MAIN LOOP
foreach my $proxy_line (@proxies_to_test) {
    last if $found_count >= $max_proxies;

    $pm->start and next; # Fork

    # CHILD PROCESS WORK
    my $result = test_proxy_complete($proxy_line);
    $pm->finish(0, $result);
}

$pm->wait_all_children;

print STDERR "━" x 50 . "\n";
print STDERR " TESTING COMPLETE!\n";
print STDERR " Found $found_count working proxies out of $total_tested tested\n";
print STDERR " Success rate: " . sprintf("%.1f", ($found_count / $total_tested * 100)) . "%\n" if $total_tested > 0;

# COMPLETE PROXY TESTING FUNCTION
sub test_proxy_complete {
    my ($proxy_line) = @_;
    chomp($proxy_line);

    # Parse proxy format: host:port:username:password OR host:port
    my ($host, $port, $username, $password) = split(":", $proxy_line);

    unless ($host && $port) {
        return { status => 'FAILED', proxy => $proxy_line, reason => 'Invalid format' };
    }

    # PING TEST
    my $ping_time = test_ping($host, $port);
    if ($ping_time < 0) {
        return { status => 'FAILED', proxy => $proxy_line, reason => 'Ping failed' };
    }

    # PORT CONNECTIVITY TEST
    unless (test_port_connection($host, $port)) {
        return { status => 'FAILED', proxy => $proxy_line, reason => 'Port not accessible' };
    }

    # HTTP/HTTPS PROXY TESTS
    my @passed_tests = ();
    my $proxy_url;

    # Build proxy URL with or without authentication
    if ($username && $password) {
        $proxy_url = "http://$username:$password\@$host:$port";
    } else {
        $proxy_url = "http://$host:$port";
    }

    # Test each URL type
    foreach my $test (@TEST_URLS) {
        if (test_proxy_url($proxy_url, $test->{url}, $test->{expected})) {
            push @passed_tests, $test->{type};
        }
    }

    # GEOLOCATION DETECTION
    my $location = detect_proxy_location($proxy_url);

    # Proxy is considered working if it passes at least one test
    if (@passed_tests > 0) {
        return {
            status => 'WORKING',
            proxy => $proxy_line,
            tests => \@passed_tests,
            ping => sprintf("%.0f", $ping_time),
            location => $location
        };
    } else {
        return {
            status => 'FAILED',
            proxy => $proxy_line,
            reason => 'All HTTP/HTTPS tests failed'
        };
    }
}

# PING TEST
sub test_ping {
    my ($host, $port) = @_;

    my $start_time = time();
    my $ping = Net::Ping->new('tcp', $ping_timeout);
    $ping->port_number($port); # Try their port

    if ($ping->ping($host)) {
        my $ping_time = (time() - $start_time) * 1000;
        return $ping_time;
    }

    return -1; # Ping failed
}

# PORT CONNECTION TEST
sub test_port_connection {
    my ($host, $port) = @_;

    my $socket = IO::Socket::INET->new(
        PeerAddr => $host,
        PeerPort => $port,
        Proto    => 'tcp',
        Timeout  => 3
    );

    if ($socket) {
        close($socket);
        return 1;
    }
    return 0;
}

# HTTP/HTTPS PROXY TEST
sub test_proxy_url {
    my ($proxy_url, $test_url, $expected_pattern) = @_;

    my $ua = LWP::UserAgent->new(
        timeout => $timeout,
        agent => 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36',
        max_redirect => 3,
        ssl_opts => { verify_hostname => 0 }  # Skip SSL verification for HTTPS
    );

    # Set proxy
    $ua->proxy(['http', 'https'], $proxy_url);

    # Make request
    my $response = $ua->get($test_url);

    if ($response->is_success) {
        my $content = $response->decoded_content;
        return ($content =~ /$expected_pattern/) ? 1 : 0;
    }

    return 0;
}

# GEOLOCATION DETECTION
sub detect_proxy_location {
    my ($proxy_url) = @_;

    my $ua = LWP::UserAgent->new(
        timeout => 5,
        agent => 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
    );

    $ua->proxy(['http', 'https'], $proxy_url);

    # Try to get location from ipinfo.io
    my $response = $ua->get('http://ipinfo.io/json');

    if ($response->is_success) {
        my $content = $response->decoded_content;
        if ($content =~ /"country":\s*"([^"]+)".*"city":\s*"([^"]+)"/) {
            return "$2, $1";
        } elsif ($content =~ /"country":\s*"([^"]+)"/) {
            return $1;
        }
    }

    return "Unknown";
}

# HELP MESSAGE
sub print_help {
    print <<'EOF';
PROXY CHECKER v1.1

Usage: cat proxies.txt | perl proxy_checker.pl [options]

Proxy Formats:
  host:port                    (No authentication)
  host:port:username:password  (With authentication)

Options:
  --timeout SEC    Request timeout (default: 5)
  --max NUM        Maximum working proxies to find (default: 10)
  --threads NUM    Worker threads (default: 5)
  --ping SEC       Ping timeout (default: 3)
  --verbose        Show detailed output
  --help           Show this help

Output Format:
  proxy_line|ping_ms|location|test_results

Tests Performed:
  ✅ Ping test
  ✅ Port connectivity
  ✅ HTTP proxy test
  ✅ HTTPS proxy test
  ✅ Location detection

Examples:
  # Test Webshare proxies
  cat webshare_proxies.txt | perl proxy_checker.pl --max 10 --verbose

  # Test with custom timeout
  echo "1.2.3.4:8080:user:pass" | perl proxy_checker.pl --timeout 15

  # Fast scan
  cat proxy_list.txt | perl proxy_checker.pl --threads 30 --timeout 5

For your proxies, use format:
142.111.48.253:7030:vzqmipmu:55hgeqyrbi3y

EOF
}